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

package com.github.steveash.synthrec.stat;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.Well19937c;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.truth.Truth;

/**
 * @author Steve Ash
 */
public class ConcurrentCounterTest {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentCounterTest.class);
    private static final int LABELCOUNT = 5;

    @Test
    public void shouldDoParallel() throws Exception {
        List<Pair<Integer,Integer>> adds = Lists.newArrayList();
        Well19937c rand = new Well19937c();

        int[] expecteds = new int[LABELCOUNT];
        for (int i = 0; i < 1_000_000; i++) {
            int label = rand.nextInt(LABELCOUNT);
            int count = rand.nextInt(1_000);
            expecteds[label] += count;
            adds.add(Pair.of(label, count));
        }
        Collections.shuffle(adds);
        ConcurrentCounter<Integer> counter = new ConcurrentCounter<>();
        adds.parallelStream().forEach(p -> counter.add(p.getKey(), p.getValue()));
        MutableMultinomial<Integer> multi = counter.drainTo();

        for (int i = 0; i < LABELCOUNT; i++) {
            double val = multi.get(i);
            log.info("Class " + i + " got " + val);
            Truth.assertWithMessage("bad on label %s", i).that(val).isWithin(0.1).of(expecteds[i]);
        }
    }
}