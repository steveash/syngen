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

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable;

/**
 * @author Steve Ash
 */
public class SamplingJointTest {
    private static final Logger log = LoggerFactory.getLogger(SamplingJointTest.class);

    private SamplingJoint joint;
    private CsvTable table;
    private RandomGenerator rand;

    @Before
    public void setUp() throws Exception {
        table = CsvTable.loadResource("jointsample.csv").build();
        joint = new SamplingJoint(table);
        rand = new JDKRandomGenerator(0xCAFF232E);
    }

    @Test
    public void shouldSampleA() throws Exception {
        MutableMultinomial<String> condA = sample1k("A");
        log.info("Conditioned on A: " + condA.normalize().toString());
        assertEquals("STEVEA", condA.best());
    }

    @Test
    public void shouldSampleB() throws Exception {
        MutableMultinomial<String> cond = sample1k("B");
        log.info("Conditioned on B: " + cond.normalize().toString());
        assertEquals("STEVEE", cond.best());
    }

    @Test
    public void shouldSampleC() throws Exception {
        MutableMultinomial<String> cond = sample1k("C");
        log.info("Conditioned on C: " + cond.normalize().toString());
        assertEquals("STEVEJ", cond.best());
    }

    @Test
    public void shouldUniform() throws Exception {
        MutableMultinomial<String> cond = new MutableMultinomial<>(table.estimateRowCount().get());
        for (int i = 0; i < 1000; i++) {
            String val = joint.sampleUniform(rand);
            cond.add(val, 1.0);
        }
        log.info("Uniform dist " + cond.normalize().toString());
        assertEquals(0.10, cond.bestProbability(), 0.02);
    }

    private MutableMultinomial<String> sample1k(String condOn) {
        MutableMultinomial<String> cond = new MutableMultinomial<>(table.estimateRowCount().get());
        for (int i = 0; i < 1000; i++) {
            String val = joint.sampleWeighted(rand, condOn);
            cond.add(val, 1.0);
        }
        return cond;
    }
}