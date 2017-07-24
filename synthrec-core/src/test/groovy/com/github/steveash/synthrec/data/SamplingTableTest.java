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

package com.github.steveash.synthrec.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.SamplingTable;

import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap.BasicEntry;

/**
 * @author Steve Ash
 */
public class SamplingTableTest {
    private static final Logger log = LoggerFactory.getLogger(SamplingTableTest.class);

    private SamplingTable<String> table;
    private RandomGenerator rand;

    @Before
    public void setUp() throws Exception {
        table = SamplingTable.createFromCountEntries(Arrays.asList(
                make("b", 60),
                make("a", 100),
                make("d", 10),
                make("c", 30)
        ));
        rand = new JDKRandomGenerator(0xEEFF1232);
    }

    @Test
    public void shouldSampleUniformly() throws Exception {
        MutableMultinomial<String> dens = new MutableMultinomial<>(4);
        for (int i = 0; i < 1000; i++) {
            dens.add(table.sampleUniform(rand), 1);
        }
        dens.normalize();
        assertEquals(0.25, dens.get("a"), 0.03);
        assertEquals(0.25, dens.get("b"), 0.03);
        assertEquals(0.25, dens.get("c"), 0.03);
        assertEquals(0.25, dens.get("d"), 0.03);
        log.info("Uniform " + dens.toString());
    }

    @Test
    public void shouldSampleWeighted() throws Exception {
        MutableMultinomial<String> dens = new MutableMultinomial<>(4);
        for (int i = 0; i < 1000; i++) {
            dens.add(table.sampleWeighted(rand), 1);
        }
        dens.normalize();
        assertEquals(0.50, dens.get("a"), 0.03);
        assertEquals(0.30, dens.get("b"), 0.03);
        assertEquals(0.15, dens.get("c"), 0.03);
        assertEquals(0.05, dens.get("d"), 0.03);
        log.info("Weighted " + dens.toString());
    }

    @Test
    public void shouldSampleMultinomial() throws Exception {
        MutableMultinomial<String> input = new MutableMultinomial<>(4);
        input.add("a", 125);
        input.add("b", 64);
        input.add("c", 32);
        input.add("d", 1);
        SamplingTable<String> sampling = SamplingTable.createFromMultinomial(input);
        MutableMultinomial<String> dens = new MutableMultinomial<>(4);
        for (int i = 0; i < 10000; i++) {
            dens.add(sampling.sampleWeighted(rand), 1);
        }
        dens.normalize();
        double dist = input.normalize().jensonShannonDivergence(dens);
        log.info("Weighted " + dens.toString());
        assertEquals(0, dist, 0.0001);
    }

    @Test
    public void shouldSampleZipfian() {
        ZipfDistribution dist = new ZipfDistribution(100, 1.2);
        MutableMultinomial<String> empir = new MutableMultinomial<>(100);
        for (int i = 0; i < 100_000; i++) {
            int sample = dist.sample();
            empir.add("A" + sample, 1);
        }
        SamplingTable<String> table = SamplingTable.createFromCountEntries(empir.nonZeroEntries(
                (s, d) -> new BasicEntry<String>(s, (int) d))
        );
        empir.normalize();
        log.info("input distrib looks like " + empir.toString());
        MutableMultinomial<String> sampled = new MutableMultinomial<>(100);
        for (int i = 0; i < 100_000; i++) {
            sampled.add(table.sampleWeighted(rand), 1);
        }
        sampled.normalize();
        log.info("sampled distrib looks like " + sampled.toString());
        double jsd = sampled.jensonShannonDivergence(empir);
        log.info("JensonShannon distance = " + jsd);
        assertTrue(jsd < 0.01);

    }

    private BasicEntry<String> make(String label, int val) {return new BasicEntry<>(label, val);}
}