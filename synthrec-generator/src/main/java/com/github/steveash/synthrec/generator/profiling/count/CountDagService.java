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

package com.github.steveash.synthrec.generator.profiling.count;

import static com.github.steveash.synthrec.Constants.ADDRESS_CITY_BIN;
import static com.github.steveash.synthrec.Constants.ADDRESS_STATE;
import static com.github.steveash.synthrec.Constants.ADDRESS_STREET_STRUCT;
import static com.github.steveash.synthrec.Constants.AGE_YEARS;
import static com.github.steveash.synthrec.Constants.FAMILY_NAMEISH;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME_STRUCT;
import static com.github.steveash.synthrec.Constants.GIVEN_FAMILY_JOINT;
import static com.github.steveash.synthrec.Constants.GIVEN_NAMEISH;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME_STRUCT;
import static com.github.steveash.synthrec.Constants.ORIGIN_CULTURE;
import static com.github.steveash.synthrec.Constants.PHONE;
import static com.github.steveash.synthrec.Constants.SEX;
import static com.github.steveash.synthrec.Constants.SSN_PATTERN;

import java.util.Set;

import com.github.steveash.synthrec.count.CountAssignment;
import com.github.steveash.synthrec.count.CountAssignment.Builder;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountFactor;
import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.Multinomial;
import com.google.common.collect.ImmutableSet;

/**
 * Creates a count DAG of the elements that we care to estimate conditional distributions from real data
 * @author Steve Ash
 */
@LazyComponent
public class CountDagService {

    // the smallest normalized entropy to consider using for a distribution rather than just taking the best
    private static final double MIN_NORM_ENTROPY = 0.25;

    public CountDag makeCountDag() {
        CountDag dag = new CountDag();
        CountFactor race = dag.newFactor(ORIGIN_CULTURE);
        CountFactor sex = dag.newFactor(SEX);
        CountFactor age = dag.newFactor(AGE_YEARS);
        CountFactor gnCulture = dag.newFactorWithParents(GIVEN_NAME_CULTURE, ORIGIN_CULTURE);
        CountFactor fnCulture = dag.newFactorWithParents(FAMILY_NAME_CULTURE, ORIGIN_CULTURE);
        CountFactor gnStruct = dag.newFactorWithParents(GIVEN_NAME_STRUCT, GIVEN_NAME_CULTURE, SEX);
        CountFactor fnStruct = dag.newFactorWithParents(FAMILY_NAME_STRUCT, FAMILY_NAME_CULTURE, SEX);
        CountFactor state = dag.newFactor(ADDRESS_STATE);
        CountFactor cityBin = dag.newFactorWithParents(ADDRESS_CITY_BIN, ADDRESS_STATE);
        CountFactor streetStruct = dag.newFactorWithParents(ADDRESS_STREET_STRUCT, ADDRESS_CITY_BIN);
        CountFactor phonePatt = dag.newFactor(PHONE);
        CountFactor ssnPatt = dag.newFactor(SSN_PATTERN);
        CountFactor givName = dag.newFactorWithParents(GIVEN_NAMEISH, GIVEN_NAME_CULTURE, SEX);
        CountFactor famName = dag.newFactorWithParents(FAMILY_NAMEISH, FAMILY_NAME_CULTURE);

        dag.markFactorSensitive(gnStruct);
        dag.markFactorReduced(gnStruct);
        dag.markFactorSensitive(givName);
        dag.markFactorReduced(givName);
        dag.markFactorSensitive(fnStruct);
        dag.markFactorReduced(fnStruct);
        dag.markFactorSensitive(famName);
        dag.markFactorReduced(famName);
        dag.markFactorSensitive(streetStruct);
        dag.markFactorReduced(streetStruct);
        dag.markFactorSensitive(phonePatt);

        dag.countIndependant(race);
        dag.countIndependant(sex);
        dag.countIndependant(age);
        dag.countIndependant(gnCulture);
        dag.countIndependant(fnCulture);
        dag.countIndependant(gnStruct);
        dag.countIndependant(fnStruct);
        dag.countIndependant(state);
        dag.countIndependant(cityBin);
        dag.countIndependant(streetStruct);
        dag.countIndependant(phonePatt);
        dag.countIndependant(ssnPatt);
        dag.countIndependant(givName);
        dag.countIndependant(famName);

        dag.countJoint(GIVEN_FAMILY_JOINT, gnCulture, fnCulture);

        dag.freeze();
        return dag;
    }

    public DagAssigner makeAssigner(CountDag dag) {
        return new DagAssigner(dag.factorNames());
    }

    public static class DagAssigner {
        private final ImmutableSet<String> factorNames;

        private DagAssigner(Set<String> factorNames) {this.factorNames = ImmutableSet.copyOf(factorNames);}

        public CountAssignment makeAssignmentFor(Record record) {
            Builder builder = CountAssignment.builder();
            applyAllMatching(builder, record);
            return builder.build();
        }

        private void applyAllMatching(Builder builder, Record record) {
            for (String name : factorNames) {
                Object feature = record.getFeatureByKey(name, null);
                if (feature != null) {
                    if (feature instanceof Multinomial) {
                        Multinomial<?> density = (Multinomial<?>) feature;
                        if (density.entropyPercOfMax() > MIN_NORM_ENTROPY) {
                            builder.putUnobserved(name, density);
                        } else {
                            builder.putObserved(name, density.best());
                        }
                    } else {
                        builder.putObserved(name, feature);
                    }
                    continue;
                }
                Object field = record.getNormal(name, null);
                if (field != null) {
                    builder.putObserved(name, field);
                    continue;
                }
            }
        }
    }
}
