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

package com.github.steveash.synthrec.generator.enrich;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Lazy;

import com.github.steveash.guavate.Guavate;
import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.CompositeFeatureComputer;
import com.github.steveash.synthrec.domain.FeatureComputer;
import com.github.steveash.synthrec.domain.FeatureComputer.FeatureKey;
import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.generator.feature.AddressParserFeature;
import com.github.steveash.synthrec.generator.feature.AddressStructureFeature;
import com.github.steveash.synthrec.generator.feature.AgeYearsFeature;
import com.github.steveash.synthrec.generator.feature.CityBinFeature;
import com.github.steveash.synthrec.generator.feature.DobParsedFeature;
import com.github.steveash.synthrec.generator.feature.FamilyNameValueFeature;
import com.github.steveash.synthrec.generator.feature.GivenNameValueFeature;
import com.github.steveash.synthrec.generator.feature.NameCultureFeature;
import com.github.steveash.synthrec.generator.feature.NameParserFeature;
import com.github.steveash.synthrec.generator.feature.NameStructureFeature;
import com.github.steveash.synthrec.generator.feature.PatternFeature;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Enriches a stream of records with features if they aren't alrady in there
 * @author Steve Ash
 */
@LazyComponent
public class FeatureService {

    @Autowired private NameParserFeature nameParserFeature;
    @Autowired private NameCultureFeature nameCultureFeature;
    @Autowired private NameStructureFeature nameStructureFeature;
    @Autowired private GivenNameValueFeature givenNameValueFeature;
    @Autowired private FamilyNameValueFeature familyNameValueFeature;
    @Autowired private AddressParserFeature addressParserFeature;
    @Autowired private AddressStructureFeature addressStructureFeature;
    @Autowired private CityBinFeature cityBinFeature;
    @Autowired private DobParsedFeature dobParsedFeature;
    @Autowired private AgeYearsFeature ageYearsFeature;

    private volatile ImmutableList<FeatureComputer> orderedFeatures;
    private volatile ImmutableSet<FeatureKey<?>> allSatisfied;

    @PostConstruct
    protected void setupFeatures() {
        // this determines which features will be executed; they will be reordered to satisfy their dependencies
        ArrayList<FeatureComputer> features = Lists.newArrayList();
        features.add(nameParserFeature);
        features.add(nameCultureFeature);
        features.add(nameStructureFeature);
        features.add(givenNameValueFeature);
        features.add(familyNameValueFeature);
        features.add(addressParserFeature);
        features.add(addressStructureFeature);
        features.add(cityBinFeature);
        features.add(dobParsedFeature);
        features.add(ageYearsFeature);
//        features.add(makePatternFor(Constants.PHONE, Constants.PHONE_PATTERN));
        features.add(makePatternFor(Constants.SSN, Constants.SSN_PATTERN));

        orderedFeatures = orderFeatures(features);
        allSatisfied = features.stream()
                .flatMap(f -> f.satisfies().stream())
                .collect(Guavate.toImmutableSet());
    }

    @Lookup
    protected PatternFeature makePatternFor(String fieldName, String featureKey) {
        throw new IllegalStateException();
    }

    private static ImmutableList<FeatureComputer> orderFeatures(ArrayList<FeatureComputer> pending) {
        ArrayList<FeatureComputer> output = Lists.newArrayList();
        Set<FeatureKey<?>> satisfied = Sets.newHashSet();

        while (true) {
            // while there are features left, lets pick them off as deps are satisfied
            Iterator<FeatureComputer> iter = pending.iterator();
            boolean addedSome = false;
            while (iter.hasNext()) {
                FeatureComputer next = iter.next();
                if (next.requires().isEmpty() || satisfied.containsAll(next.requires())) {
                    // this is ready to be added
                    output.add(next);
                    Set<FeatureKey<?>> satisfies = next.satisfies();
                    Preconditions.checkState(!satisfies.isEmpty(), "cant satisfy nothing");
                    satisfied.addAll(satisfies);
                    addedSome = true;
                    iter.remove();
                }
            }
            if (addedSome) {
                continue;
            }
            if (pending.isEmpty()) {
                break;
            }
            throw new IllegalStateException("cant satisfy dependencies for features " + pending);
        }
        return ImmutableList.copyOf(output);
    }

    public ImmutableSet<FeatureKey<?>> allSatisfied() {
        return allSatisfied;
    }

    public Record enrichRecord(Record rec) {
        for (FeatureComputer feature : orderedFeatures) {
            feature.emitFeatures(rec, rec);
        }
        return rec;
    }

    public CompositeFeatureComputer computersForAsComposite(Set<FeatureKey<?>> asks) {
        return new CompositeFeatureComputer(computersFor(asks));
    }

    public List<FeatureComputer> computersFor(Set<FeatureKey<?>> asks) {
        HashSet<FeatureKey<?>> reqs = Sets.newHashSet(asks);
        HashSet<FeatureKey<?>> sats = Sets.newHashSet();
        SetView<FeatureKey<?>> needed = Sets.difference(reqs, sats);
        Set<FeatureComputer> comps = Sets.newIdentityHashSet();
        while (!needed.isEmpty()) {
            FeatureKey<?> next = Iterables.getFirst(needed, null);
            for (FeatureComputer computer : orderedFeatures) {
                if (!comps.contains(computer) && computer.satisfies().contains(next)) {
                    comps.add(computer);
                    sats.addAll(computer.satisfies());
                    reqs.addAll(computer.requires());
                }
            }
        }
        return orderFeatures(Lists.newArrayList(comps));
    }
}
