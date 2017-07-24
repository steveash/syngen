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

package com.github.steveash.synthrec.count;

import static com.github.steveash.synthrec.Constants.ADDRESS_CITY_BIN;
import static com.github.steveash.synthrec.Constants.ADDRESS_STATE;
import static com.github.steveash.synthrec.Constants.ADDRESS_STREET_STRUCT;
import static com.github.steveash.synthrec.Constants.AGE_YEARS;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME_STRUCT;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME_STRUCT;
import static com.github.steveash.synthrec.Constants.ORIGIN_CULTURE;
import static com.github.steveash.synthrec.Constants.PHONE_PATTERN;
import static com.github.steveash.synthrec.Constants.SEX;
import static com.github.steveash.synthrec.Constants.SSN_PATTERN;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.count.CountAssignment.Builder;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

/**
 * @author Steve Ash
 */
public class CountDagTest {
    private static final Logger log = LoggerFactory.getLogger(CountDagTest.class);

    @Test
    public void shouldCountSimple() throws Exception {
        CountDag dag = new CountDag();
        CountFactor a = dag.newFactor("A");
        CountFactor b = dag.newFactor("B");
        dag.countIndependant(a);
        dag.countIndependant(b);
        dag.freeze();
        CountAssignment assign1 = make("A", "aa", "B", "bb");
        dag.add(assign1);
        dag.add(assign1);
        CountAssignment assign2 = make("A", "aaa", "B", "bbb");
        dag.add(assign2);
        dag.add(assign2);
        dag.add(assign2);
        CountAssignment assign3 = make("A", "aa", "B", "b");
        dag.add(assign3);

        MutableMultinomial<?> as = a.makeJoint();
        assertEquals(2, as.size());
        MutableMultinomial<?> bs = b.makeJoint();
        assertEquals(3, bs.size());
        log.info("A DISTRIB: " + as.normalize());
        log.info("B DISTRIB: " + bs.normalize());
    }

    @Test
    public void shouldCountConditional1() throws Exception {
        CountDag dag = new CountDag();
        CountFactor a = dag.newFactor("A");
        CountFactor b = dag.newFactorWithParents("B", "A");
        dag.countIndependant(a);
        dag.countIndependant(b);
        dag.freeze();
        CountAssignment assign1 = make("A", "aa", "B", "b");
        dag.add(assign1);
        dag.add(assign1);
        CountAssignment assign2 = make("A", "aaa", "B", "bb");
        dag.add(assign2);
        dag.add(assign2);
        dag.add(assign2);
        CountAssignment assign3 = make("A", "aa", "B", "bb");
        dag.add(assign3);

        MutableMultinomial<?> as = a.makeJoint();
        assertEquals(2, as.size());
        MutableMultinomial<?> bs = b.makeJoint();
        assertEquals(3, bs.size());
        log.info("A DISTRIB: " + as.normalize());
        log.info("B DISTRIB: " + bs.normalize());
    }

    @Test
    public void shouldCountConditional2() throws Exception {
        CountDag dag = new CountDag();
        CountFactor a = dag.newFactor("A");
        CountFactor b = dag.newFactorWithParents("B", "A");
        CountFactor c = dag.newFactorWithParents("C", "A");
        dag.countIndependant(a);
        CountFactor groupFactor = dag.countJoint("", b, c);
        dag.freeze();

        CountAssignment assign1 = make("A", "aa", "B", "b", "C", "cc");
        dag.add(assign1);
        dag.add(assign1);
        CountAssignment assign2 = make("A", "aaa", "B", "bb", "C", "cc");
        dag.add(assign2);
        dag.add(assign2);
        dag.add(assign2);
        CountAssignment assign3 = make("A", "aa", "B", "bb", "C", "cc");
        dag.add(assign3);

        // now add an assignment without total certainty
        Builder builder = CountAssignment.builder();
        builder.putObserved("B", "bbbb");
        builder.putUnobserved("A", Multinomial.makeNormalizedFrom("aa", "aa", "aa", "aaa", "a"));
        builder.putUnobserved("C", Multinomial.makeNormalizedFrom("cc", "cc", "cc", "cc", "c"));
        dag.add(builder.build());

        MutableMultinomial<DehydratedAssignment> as = a.makeJoint().normalize();
        log.info("A DISTRIB: " + as);
        assertEquals(0.028, as.get(dag.dehydrate(AssignmentInstance.make("A", "a"))), 0.001);
        assertEquals(0.514, as.get(dag.dehydrate(AssignmentInstance.make("A", "aa"))), 0.001);
        assertEquals(0.457, as.get(dag.dehydrate(AssignmentInstance.make("A", "aaa"))), 0.001);

        MutableMultinomial<DehydratedAssignment> cs = groupFactor.makeJoint();
        log.info("C DISTRIB: " + cs.normalize());
        assertEquals(9, cs.size());

        File file = File.createTempFile("tempCountdagOut", ".dat");
        log.info("Writing countdag serialized form to " + file.getAbsolutePath());
//        file.deleteOnExit();
        ReadWrite.objectToGzip(dag, file);
        CountDag readDag = ReadWrite.objectFromGzip(file.getAbsolutePath(), CountDag.class);
        CountFactor readA = readDag.getFactor("A");
        MutableMultinomial<DehydratedAssignment> readAs = readA.makeJoint().normalize();
        log.info("READ A DISTRIB: " + readAs);
        assertEquals(0.028, readAs.get(readDag.dehydrate(AssignmentInstance.make("A", "a"))), 0.001);
        assertEquals(0.514, readAs.get(readDag.dehydrate(AssignmentInstance.make("A", "aa"))), 0.001);
        assertEquals(0.457, readAs.get(readDag.dehydrate(AssignmentInstance.make("A", "aaa"))), 0.001);
    }

    @Test
    public void shouldSortTopological() throws Exception {
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
        CountFactor phonePatt = dag.newFactor(PHONE_PATTERN);
        CountFactor ssnPatt = dag.newFactor(SSN_PATTERN);

        dag.markFactorSensitive(gnStruct);
        dag.markFactorSensitive(fnStruct);
        dag.markFactorSensitive(streetStruct);

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

        dag.countJoint("", gnCulture, fnCulture);

        dag.freeze();
        Joiner joiner = Joiner.on(',');
        for (FactorGroup group : dag.topologicalOrdering()) {
            log.info(joiner.join(group.getFactorNonParentsName()));
        }
    }

    private CountAssignment make(String... keyVal) {
        HashMap<String, Object> values = Maps.newHashMap();
        int i = 0;
        while (i < keyVal.length) {
            values.put(keyVal[i], keyVal[i + 1]);
            i += 2;
        }
        return CountAssignment.fromObserved(values);
    }
}