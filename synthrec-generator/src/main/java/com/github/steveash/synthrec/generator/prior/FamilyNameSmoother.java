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

import static com.google.common.base.Preconditions.checkNotNull;

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
import com.github.steveash.synthrec.name.culture.CultureDetector;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.PrototypeComponent;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.stat.BackoffSampler;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Prototype that owns the smoothing process for sampling family tokens.
 * @author Steve Ash
 * @see GivenNameSmoother for more general info; this is the same except no sex stuff
 */
@PrototypeComponent
public class FamilyNameSmoother {
    private static final Logger log = LoggerFactory.getLogger(FamilyNameSmoother.class);

    private static final ImmutableSet<String> CULTURE_ONLY = ImmutableSet.of(Constants.FAMILY_NAME_CULTURE);

    // these are just guesses from the priors
    private static final ImmutableMap<String, Integer> DEFAULT_CULTURE_EXP_SIZE = ImmutableMap.<String, Integer>builder()
            .put("BRITISH", 1168099)
            .put("HISPANIC", 405538)
            .put("GREEK", 352202)
            .put("MUSLIM", 321439)
            .put("AFRICAN", 205494)
            .put("JEWISH", 192225)
            .put("EAST ASIA", 151500)
            .put("EAST EUROPE", 150252)
            .put("NORDIC", 137048)
            .put("INDIA", 123584)
            .put("GERMAN", 116700)
            .put("NATIVE AMERICAN", 88593)
            .put("FRENCH", 67084)
            .put("ITALIAN", 65934)
            .put("JAPAN", 13051)
            .build();

    @Resource private GenRecordsConfig genRecordsConfig;
    @Resource private CultureDetector cultureDetector;

    private final FactorGroup familyNameishGroup;
    private final Set<String> empiricalCultures;

    private EmpPriorSmoother smoother;

    private Multinomial<String> priorFamilyName;
    private Map<AssignmentInstance, MutableMultinomial<String>> empNameGivenCulture;
    private Sampler<String> backoff;

    public FamilyNameSmoother(CountDag countDag) {
        familyNameishGroup = countDag.getFactorGroup(Constants.FAMILY_NAMEISH);
        FactorGroup cultureGroup = countDag.getFactorGroup(Constants.FAMILY_NAME_CULTURE);
        Preconditions.checkArgument(familyNameishGroup.getFactorParentsNameAsSet().equals(CULTURE_ONLY));
        MutableMultinomial<String> empiricalCulture = Marginalizer.marginalize(cultureGroup.makeConditionalCopy());
        empiricalCultures = Sets.newHashSet(empiricalCulture.keySet());
        log.info("Family name smoother sees that empirical distribution of " + empiricalCultures.size());
    }

    @PostConstruct
    protected void setup() {
        smoother = new EmpPriorSmoother(genRecordsConfig.getDefaultPriorAlpha(),
                genRecordsConfig.getDefaultPriorMinVirtual(),
                genRecordsConfig.isConditionalFamilyNameOnlyEmitCommon()
        );
    }

    public void emitGivenNameSamplers(Map<NamePart, ISampler<String>> sink) {
        createNamePriors();

        empNameGivenCulture = familyNameishGroup.makeConditionalCopy();
        MutableMultinomial<String> empName = Marginalizer.marginalize(empNameGivenCulture);
        backoff = SamplingTable.createFromMultinomial(empName);

        Function<AssignmentInstance, Sampler<String>> familyFactory = ai -> {
            String culture = checkNotNull(ai.getString(Constants.FAMILY_NAME_CULTURE, null));
            return Iterables.getOnlyElement(makeForCultures(ImmutableSet.of(culture)).values());
        };
        LazyMap<AssignmentInstance, Sampler<String>> map = LazyMap.make(familyFactory);
        // preload the cultures that we are likely to see since we can just run the culture tagger once that way
        map.putAll(makeForCultures(empiricalCultures));

        ConditionalSampler<String> sampler = new BackoffSampler<>(map, backoff, CULTURE_ONLY);
        emitForApplicable(sink, sampler);
    }

    private Map<AssignmentInstance, Sampler<String>> makeForCultures(Set<String> cultures) {
        log.info("Creating sampler for family names for cultures " + cultures + "...");
        Stopwatch watch = Stopwatch.createStarted();
        Map<String, MutableMultinomial<String>> targets = makeEmptyOutsFor(cultures);
        // collect all of the priors (on the fly)
        priorFamilyName.entries().parallelStream().forEach(next -> {

            Multinomial<String> cultureDist = cultureDetector.detectSingleToken(next.getKey());
            for (String culture : cultures) {
                double probCult = cultureDist.get(culture);
                if (probCult > genRecordsConfig.getNameCultureMinProb()) {
                    MutableMultinomial<String> target = targets.get(culture);
                    synchronized (target) {
                        target.add(next.getKey(), next.getDoubleValue() * probCult);
                    }
                }
            }
        });
        // now smooth with the empirical
        Map<AssignmentInstance, Sampler<String>> results = Maps.newHashMapWithExpectedSize(cultures.size());
        for (String culture : cultures) {
            MutableMultinomial<String> smoothed = targets.get(culture);
            AssignmentInstance ai = AssignmentInstance.make(Constants.FAMILY_NAME_CULTURE, culture);
            MutableMultinomial<String> maybeEmp = empNameGivenCulture.get(ai);
            if (maybeEmp != null) {
                smoother.smoothPriorCopy(maybeEmp, smoothed, "familyNameCulture-" + culture);
            }
            Sampler<String> sampler;
            if (smoothed.size() < genRecordsConfig.getNameCultureMinEntries()) {
                sampler = VoidSampler.getInstance();
            } else {
                log.info(".. smoothed family name distribution for " + culture + " has " + smoothed.size() + " entries");
                sampler = SamplingTable.createFromMultinomial(smoothed);
            }
            results.put(ai, sampler);
        }
        watch.stop();
        log.info("...finished creating family name dist for " + cultures + " in " + watch);
        return results;
    }

    private Map<String, MutableMultinomial<String>> makeEmptyOutsFor(Set<String> cultures) {
        Map<String, MutableMultinomial<String>> outs = Maps.newHashMapWithExpectedSize(cultures.size());
        for (String culture : cultures) {
            Integer expectedCount = DEFAULT_CULTURE_EXP_SIZE.getOrDefault(culture, 60_000);
            outs.put(culture, MutableMultinomial.createUnknownMax(expectedCount));
        }
        return outs;
    }

    private void emitForApplicable(Map<NamePart, ISampler<String>> sink,
            ConditionalSampler<String> sampler
    ) {
        for (NamePart namePart : NamePart.values()) {
            if (NamePart.isSurnameIdentifying(namePart)) {
                sink.put(namePart, sampler);
            }
        }
    }

    private void createNamePriors() {
        log.info("Loading family name priors...");
        this.priorFamilyName = ReadWrite.loadCountTableAsMultinomial(
                "names/dmf/name.Surname.freq.clob", Normalizers.interner()::intern).toImmutable();
    }
}

