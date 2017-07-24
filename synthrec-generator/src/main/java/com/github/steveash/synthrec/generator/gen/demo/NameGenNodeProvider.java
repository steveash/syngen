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

package com.github.steveash.synthrec.generator.gen.demo;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.demo.NonNameGenerator;
import com.github.steveash.synthrec.generator.prior.FamilyNameSmoother;
import com.github.steveash.synthrec.generator.prior.GivenNameSmoother;
import com.github.steveash.synthrec.generator.prior.NameCounts;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.EnglishWords;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.name.NameStopWords;
import com.github.steveash.synthrec.stat.ConditionalSampler;
import com.github.steveash.synthrec.stat.EmpPriorSmoother;
import com.github.steveash.synthrec.stat.EmptySampler;
import com.github.steveash.synthrec.stat.ISampler;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.github.steveash.synthrec.stat.SequenceConditionalSampler;
import com.github.steveash.synthrec.stat.VoidSampler;
import com.github.steveash.synthrec.string.PatternExpander;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Gen node that pulls the given + surname structure from the gen dag and then builds up
 * a name.  The field sketches are expected to be keyed by the namePart+entryfield (see
 * NameStructureFeature#getNamePartFromLabel (etc))
 * Whenever the two (independent) samples both write to the same entry field -- we append it
 * to the field
 * @author Steve Ash
 */
@LazyComponent
public class NameGenNodeProvider implements GenNodeProvider {
    private static final Logger log = LoggerFactory.getLogger(NameGenNodeProvider.class);

    @Resource private NameStopWords nameStopWords;
    @Resource private EnglishWords englishWords;
    @Resource private NonNameGenerator nonNameGenerator;
    @Resource private GenRecordsConfig genRecordsConfig;

    private EmpPriorSmoother smoother;

    @PostConstruct
    protected void setup() {
        smoother = new EmpPriorSmoother(genRecordsConfig.getDefaultPriorAlpha(),
                genRecordsConfig.getDefaultPriorMinVirtual()
        );
    }

    @Override
    public GenNode makeFor(String name, CountDag countDag) {
        log.info("NameGenNodeProvider called for " + name + " and creating name node...");
        PatternExpander expander = new PatternExpander(3, NameCounts.loadAllWords(nameStopWords, englishWords));
        Map<NamePart, ISampler<String>> parts = Maps.newHashMap();
        addSimpleParts(parts, countDag);
        addSpecialParts(parts);
        makeGivenSmoother(countDag).emitGivenNameSamplers(parts);
        makeFamilySmoother(countDag).emitGivenNameSamplers(parts);
        throwIfMissingSomeParts(parts);
        return new NameGenNode(parts, expander, r -> nonNameGenerator.phrase(r, 1).get(0));
    }

    private void addSpecialParts(Map<NamePart, ISampler<String>> sink) {
        // there are a few place holders that we dont want to sample from but to satisfy the missing some
        // parts check:
        ConditionalSampler<String> voids = ConditionalSampler.adaptSampler(VoidSampler.getInstance());
        ConditionalSampler<String> empty = ConditionalSampler.adaptSampler(EmptySampler.INSTANCE);
        sink.putIfAbsent(NamePart.Duplicate, voids); // this is handled specially in the name gen node
        sink.putIfAbsent(NamePart.Nickname, empty);
        sink.putIfAbsent(NamePart.Particle, empty);
        sink.putIfAbsent(NamePart.Unknown, empty);

        SequenceConditionalSampler<String> nonNameSampler = (r, c, a) -> nonNameGenerator.phrase(r, c);
        sink.putIfAbsent(NamePart.NonNamePhrase, nonNameSampler);
    }

    @Lookup
    protected GivenNameSmoother makeGivenSmoother(CountDag countDag) {
        throw new IllegalStateException("spring shouldve proxied");
    }

    @Lookup
    protected FamilyNameSmoother makeFamilySmoother(CountDag countDag) {
        throw new IllegalStateException("spring shouldve proxied");
    }

    private void addSimpleParts(Map<NamePart, ISampler<String>> sink, CountDag countDag) {
        Map<String, MutableMultinomial<Object>> givenCounts = countDag.getFactorStats(Constants.GIVEN_NAME_STRUCT)
                .makeSubfieldUnaryCopy();
        Map<String, MutableMultinomial<Object>> familyCounts = countDag.getFactorStats(Constants.FAMILY_NAME_STRUCT)
                .makeSubfieldUnaryCopy();

        for (NamePart namePart : NameCounts.TAG_TO_RESOURCE.keySet()) {
            MutableMultinomial<String> prior = NameCounts.loadMultinomialForTag(namePart);
            MutableMultinomial<String> empir = MutableMultinomial.createUnknownMax();
            MutableMultinomial<Object> maybeGiven = givenCounts.get(namePart.toString());
            if (maybeGiven != null) empir.addMultinomial(maybeGiven);
            MutableMultinomial<Object> maybeFamily = familyCounts.get(namePart.toString());
            if (maybeFamily != null) empir.addMultinomial(maybeFamily);
            if (empir.isNotEmpty()) {
                smoother.smoothPriorCopy(empir, prior, namePart.toString());
            }
            sink.put(namePart, ConditionalSampler.adaptSampler(SamplingTable.createFromMultinomial(prior)));
        }
    }

    private void throwIfMissingSomeParts(Map<NamePart, ISampler<String>> parts) {
        SetView<NamePart> missing = Sets.difference(Sets.newHashSet(NamePart.values()), parts.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Somehow we didnt create samplers for every name part; missing " + missing);
        }
    }

    @Override
    public Set<String> providesForNames() {
        return NameGenNode.NAME_CONST_TO_ENUM.keySet();
    }
}
