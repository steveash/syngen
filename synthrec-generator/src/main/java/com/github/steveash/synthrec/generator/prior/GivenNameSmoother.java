/*
 * Copyright (c) 2017, Steve Ash
 *
 * This file is part of Syngen.
 * Syngen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Syngen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Syngen.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.steveash.synthrec.generator.prior;

import static com.github.steveash.synthrec.Constants.GIVEN_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.SEX;
import static com.github.steveash.synthrec.Constants.SEX_FEMALE;
import static com.github.steveash.synthrec.Constants.SEX_MALE;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.canonical.Normalizers;
import com.github.steveash.synthrec.collect.LazyMap;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.demo.GenderGivenName;
import com.github.steveash.synthrec.generator.spring.PrototypeComponent;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.name.culture.CultureDetector;
import com.github.steveash.synthrec.stat.BackoffSampler;
import com.github.steveash.synthrec.stat.CompositeSampler;
import com.github.steveash.synthrec.stat.ConditionalSampler;
import com.github.steveash.synthrec.stat.EmpPriorSmoother;
import com.github.steveash.synthrec.stat.ISampler;
import com.github.steveash.synthrec.stat.Marginalizer;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.github.steveash.synthrec.stat.VoidSampler;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Prototype that owns the smoothing process for sampling name token values.  Few things:
 * 1- general strategy is to use empirical distributions that were conditionally measured using NAMEISH values
 * 2- for non unique/identifying NAMEISH values we just use the subfield empiricals
 * 3- smoothing is against the DMF for NAMEISH values and collected deidented real values for the others
 * 4- sampling NAMEISH values is conditional on culture + sex. We back into the P (name | culture, sex) by
 * modeling P(sex | name) with a large dictionary curated from public sources (DMF + profiled over dozens of
 * real data), using a predictor to model P(culture | name) and then doing the bayes transform to get the
 * target distribution
 * 5- if there aren't enough values in the smoothed joint then we backoff to P(name | sex) and if the sex
 * is not M or F (e.g. U or other) then we backoff just P(name)
 * 6- the P(name | cutlure, sex) distributions are lazyily built to avoid enumerated the whole joint
 * when we dont need to
 * @author Steve Ash
 */
@PrototypeComponent
public class GivenNameSmoother {
    private static final Logger log = LoggerFactory.getLogger(GivenNameSmoother.class);

    private static final ImmutableSet<String> CULTURE_AND_SEX = ImmutableSet.of(GIVEN_NAME_CULTURE,
            SEX
    );
    private static final ImmutableSet<String> SEX_ONLY = ImmutableSet.of(SEX);
    private static final AssignmentInstance ASSIGN_FEMALE = AssignmentInstance.make(SEX,
            SEX_FEMALE
    );
    private static final AssignmentInstance ASSIGN_MALE = AssignmentInstance.make(SEX, SEX_MALE);

    // these are all over-estimates since they are for the whole given names and not split by gender
    private static final ImmutableMap<String, Integer> DEFAULT_CULTURE_EXP_SIZE = ImmutableMap.<String, Integer>builder()
            .put("BRITISH", 652608)
            .put("GREEK", 216838)
            .put("HISPANIC", 212998)
            .put("MUSLIM", 206392)
            .put("JEWISH", 163415)
            .put("AFRICAN", 125540)
            .put("EAST ASIA", 111299)
            .put("INDIA", 94053)
            .put("EAST EUROPE", 45327)
            .put("NORDIC", 37376)
            .put("FRENCH", 36141)
            .put("NATIVE AMERICAN", 35880)
            .put("ITALIAN", 25622)
            .put("GERMAN", 25441)
            .put("JAPAN", 14330)
            .build();

    @Resource private GenRecordsConfig genRecordsConfig;
    @Resource private GenderGivenName genderGivenName;
    @Resource private CultureDetector cultureDetector;

    private final FactorGroup givenNameishGroup;
    private final Set<String> empiricalCultures;

    private EmpPriorSmoother smoother;

    private Multinomial<String> priorNameGivenSexMale;     // P(name | sex=male) multinomial
    private Multinomial<String> priorNameGivenSexFemale;   // P(name | sex=female) multinomial
    private Map<AssignmentInstance, MutableMultinomial<String>> empNameGivenCultureSex;
    private BackoffSampler<String> backoff;

    public GivenNameSmoother(CountDag countDag) {
        givenNameishGroup = countDag.getFactorGroup(Constants.GIVEN_NAMEISH);
        FactorGroup cultureGroup = countDag.getFactorGroup(GIVEN_NAME_CULTURE);
        MutableMultinomial<String> empiricalCulture = Marginalizer.marginalize(cultureGroup.makeConditionalCopy());
        empiricalCultures = Sets.newHashSet(empiricalCulture.keySet());
        log.info("Given name smoother sees that empirical distribution of " + empiricalCultures.size());
        Preconditions.checkArgument(givenNameishGroup.getFactorParentsNameAsSet().equals(CULTURE_AND_SEX));
    }

    @PostConstruct
    protected void setup() {
        smoother = new EmpPriorSmoother(genRecordsConfig.getDefaultPriorAlpha(),
                genRecordsConfig.getDefaultPriorMinVirtual(),
                genRecordsConfig.isConditionalGivenNameOnlyEmitCommon()
        );
    }

    public void emitGivenNameSamplers(Map<NamePart, ISampler<String>> sink) {
        createNameSexPriors();

        empNameGivenCultureSex = givenNameishGroup.makeConditionalCopy();
        Map<AssignmentInstance, MutableMultinomial<String>> empNameGivenSex =
                Marginalizer.marginalizeTo(SEX_ONLY, empNameGivenCultureSex);

        ArrayList<Sampler<String>> finalBackoffList = Lists.newArrayList();
        Map<AssignmentInstance, Sampler<String>> ageBackoff = Maps.newHashMap();

        emitSexMarginalBackoff(finalBackoffList, ageBackoff, ASSIGN_MALE, priorNameGivenSexMale, empNameGivenSex);
        emitSexMarginalBackoff(finalBackoffList, ageBackoff, ASSIGN_FEMALE, priorNameGivenSexFemale, empNameGivenSex);

        backoff = new BackoffSampler<>(ageBackoff, new CompositeSampler<>(finalBackoffList), SEX_ONLY);

        Function<AssignmentInstance, Sampler<String>> givenFactory = ai -> {
            String culture = checkNotNull(ai.getString(GIVEN_NAME_CULTURE, null));
            String sex = checkNotNull(ai.getString(SEX, null));
            if (!sex.equalsIgnoreCase(SEX_MALE) && !sex.equalsIgnoreCase(SEX_FEMALE)) {
                return VoidSampler.getInstance();
            }
            return tryForCultureSex(culture, ai);
        };

        LazyMap<AssignmentInstance, Sampler<String>> map = LazyMap.make(givenFactory);
        // preload the entries for the cultures we are likley to see
        map.putAll(tryForMany(empiricalCultures));

        ConditionalSampler<String> sampler = new BackoffSampler<>(map, backoff, CULTURE_AND_SEX);
        emitForApplicable(sink, sampler);
    }

    private Sampler<String> tryForCultureSex(String culture, AssignmentInstance assign) {
        Map<AssignmentInstance, Sampler<String>> many = tryForMany(ImmutableSet.of(culture));
        return checkNotNull(many.get(assign), "cant make distribution for culturesex", assign);
    }

    private Map<AssignmentInstance, Sampler<String>> tryForMany(Set<String> cultures) {
        log.info("Creating the smoothed given name distributions for " + cultures + "...");
        Stopwatch watch = Stopwatch.createStarted();
        Map<String, MutableMultinomial<String>> targetsMale = makeEmptyOutsFor(cultures);
        Map<String, MutableMultinomial<String>> targetsFemale = makeEmptyOutsFor(cultures);

        Sets.union(priorNameGivenSexMale.keySet(), priorNameGivenSexFemale.keySet()).parallelStream().forEach(next -> {
            Multinomial<String> cultureDist = cultureDetector.detectSingleToken(next);
            for (String culture : cultures) {
                double probCult = cultureDist.get(culture);
                if (probCult > genRecordsConfig.getNameCultureMinProb()) {
                    emitCultSex(targetsMale, next, culture, probCult, priorNameGivenSexMale);
                    emitCultSex(targetsFemale, next, culture, probCult, priorNameGivenSexFemale);
                }
            }
        });
        Map<AssignmentInstance, Sampler<String>> results = Maps.newHashMapWithExpectedSize(cultures.size() * 2);
        for (String culture : cultures) {
            emitSmoothed(results, targetsMale, culture, SEX_MALE);
            emitSmoothed(results, targetsFemale, culture, SEX_FEMALE);
        }
        watch.stop();
        log.info("... finished given distribution for " + cultures + " in " + watch);
        return results;
    }

    private void emitSmoothed(Map<AssignmentInstance, Sampler<String>> results,
            Map<String, MutableMultinomial<String>> targets,
            String culture,
            String sex
    ) {
        AssignmentInstance assign = AssignmentInstance.make(GIVEN_NAME_CULTURE, culture, SEX, sex);
        MutableMultinomial<String> maybeEmp = empNameGivenCultureSex.get(assign);
        MutableMultinomial<String> smoothed = targets.get(culture);
        if (maybeEmp != null) {
            smoother.smoothPriorCopy(maybeEmp, smoothed, "givenNameCultureSex-" + culture + "-" + sex);
        }
        if (smoothed.size() < genRecordsConfig.getNameCultureMinEntries()) {
            results.put(assign, VoidSampler.getInstance());
        } else {
            log.info("...smoothed given for " + culture + "-" + sex + " has " + smoothed.size() + " entries");
            results.put(assign, SamplingTable.createFromMultinomial(smoothed));
        }
    }

    private void emitCultSex(Map<String, MutableMultinomial<String>> targets,
            String name,
            String culture,
            double probCult,
            Multinomial<String> prior
    ) {
        double priorProb = prior.get(name);
        if (priorProb > 0) {
            MutableMultinomial<String> target = targets.get(culture);
            synchronized (target) {
                target.add(name, priorProb * probCult);
            }
        }
    }

    private Map<String, MutableMultinomial<String>> makeEmptyOutsFor(Set<String> cultures) {
        Map<String, MutableMultinomial<String>> outs = Maps.newHashMapWithExpectedSize(cultures.size());
        for (String culture : cultures) {
            Integer expectedCount = DEFAULT_CULTURE_EXP_SIZE.getOrDefault(culture, 30_000);
            outs.put(culture, MutableMultinomial.createUnknownMax(expectedCount));
        }
        return outs;
    }

    private void emitForApplicable(Map<NamePart, ISampler<String>> sink,
            ConditionalSampler<String> sampler
    ) {
        for (NamePart namePart : NamePart.values()) {
            if (NamePart.isGivenIdentifying(namePart)) {
                sink.put(namePart, sampler);
            }
        }
    }

    private void emitSexMarginalBackoff(List<Sampler<String>> finalBackoffList,
            Map<AssignmentInstance, Sampler<String>> ageBackoff,
            AssignmentInstance assignKey,
            Multinomial<String> prior,
            Map<AssignmentInstance, MutableMultinomial<String>> empNameGivenSex
    ) {
        MutableMultinomial<String> maybeEmpNameGivenSex = empNameGivenSex.get(assignKey);
        MutableMultinomial<String> smoothed = prior.copy();
        if (maybeEmpNameGivenSex != null) {
            smoother.smoothPriorCopy(maybeEmpNameGivenSex, smoothed, assignKey.toString());
        }
        SamplingTable<String> sampling = SamplingTable.createFromMultinomial(smoothed);
        finalBackoffList.add(sampling);
        ageBackoff.put(assignKey, sampling);
    }

    private void createNameSexPriors() {
        log.info("Loading given name priors...");
        MutableMultinomial<String> givenPriorMale = MutableMultinomial.createUnknownMax();
        MutableMultinomial<String> givenPriorFemale = MutableMultinomial.createUnknownMax();

        CsvTable table = ReadWrite.loadCountTable("names/dmf/name.GivenName.freq.clob");
        for (Row row : table) {
            String rawName = Normalizers.interner().intern(row.getString(0));
            String normalName = Names.normalize(rawName);
            double maleProb = genderGivenName.probMale(normalName);
            double femProb = 1.0 - maleProb;
            int count = row.getInt(1);
            if (maleProb > 0) {
                givenPriorMale.add(rawName, maleProb * count);
            }
            if (femProb > 0) {
                givenPriorFemale.add(rawName, femProb * count);
            }
        }
        this.priorNameGivenSexMale = givenPriorMale.toImmutable();
        this.priorNameGivenSexFemale = givenPriorFemale.toImmutable();
    }
}

