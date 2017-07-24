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
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import org.apache.commons.math3.random.Well19937c;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Ordering;
import com.google.common.truth.Truth;

import cc.mallet.util.Maths;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * @author Steve Ash
 */
public class MutableMultinomialTest {
    private static final Logger log = LoggerFactory.getLogger(MutableMultinomialTest.class);

    @Test
    public void shouldNormalize() throws Exception {
        MutableMultinomial<String> md = new MutableMultinomial<>(3);
        md.add("a", 20);
        md.add("b", 30);
        md.add("c", 50);

        Multinomial<String> finalDist = md.normalize().toImmutable();
        assertEquals(0.2, finalDist.get("a"), 0.01);
        assertEquals(0.3, finalDist.get("b"), 0.01);
        assertEquals(0.5, finalDist.get("c"), 0.01);
    }

    @Test
    public void shouldEntropy() throws Exception {
        MutableMultinomial<String> dens = new MutableMultinomial<>(3);
        dens.add("a", 10000);
        dens.add("b", 200);
        dens.add("c", 200);
        MutableMultinomial<String> lowEntropy = dens.normalize();

        MutableMultinomial<String> dens2 = new MutableMultinomial<>(3);
                dens2.add("a", 100);
                dens2.add("b", 99);
                dens2.add("c", 77);
        MutableMultinomial<String> highEntropy = dens2.normalize();
        log.info("High " + highEntropy.entropy() + " " + highEntropy.entropyPercOfMax());
        log.info("Low " + lowEntropy.entropy() + " " + lowEntropy.entropyPercOfMax());

        assertTrue(highEntropy.entropy() > lowEntropy.entropy());
    }

    @Test
    public void shouldAddEntropyScaled() throws Exception {
        MutableMultinomial<String> dens = new MutableMultinomial<>(3);
        dens.add("a", 1000);
        dens.add("b", 1);
        dens.add("c", 1);
        MutableMultinomial<String> lowEntropy = dens.normalize();

        MutableMultinomial<String> dens2 = new MutableMultinomial<>(3);
                dens2.add("a", 50);
                dens2.add("b", 50);
                dens2.add("c", 50);
        MutableMultinomial<String> highEntropy = dens2.normalize();

        MutableMultinomial<String> dens3 = new MutableMultinomial<>(3);
                dens3.add("a", 50);
                dens3.add("b", 60);
                dens3.add("c", 60);
        MutableMultinomial<String> medEntropy = dens3.normalize();

        log.info("Low " + lowEntropy.entropy());
        log.info("Med " + medEntropy.entropy());
        log.info("High " + highEntropy.entropy());

        MutableMultinomial<String> result = new MutableMultinomial<>(3);
        result.addEntropyScaled(highEntropy);
        result.addEntropyScaled(medEntropy);
        result.addEntropyScaled(lowEntropy);

        result = result.normalize();
        log.info("Result\n" + result);
        assertEquals("a", result.best());
    }

    @Test
    public void shouldKlDivergence() throws Exception {
        Random rand = new Random(0x1234FFDA);
        MutableMultinomial<Integer> d1 = new MutableMultinomial<>(100);
        MutableMultinomial<Integer> d2 = new MutableMultinomial<>(100);
        for (int i = 0; i < 100; i++) {
            d1.set(i, rand.nextInt(10_000));
            d2.set(i, rand.nextInt(10_000));
        }
        d1.normalize();
        d2.normalize();
        double mineSays = d1.kullbackLieblerTo(d2);
        log.info("KL d1||d2 is " + mineSays);
        double[] d1d = new double[100];
        double[] d2d = new double[100];
        d1.fillDensity(Ordering.natural(), d1d);
        d2.fillDensity(Ordering.natural(), d2d);
        double malletSays = Maths.klDivergence(d1d, d2d);
        log.info("Mallet says " + malletSays);
        Assert.assertEquals(malletSays, mineSays, 0.0001);
    }

    @Test
    public void shouldKlDivergence2() throws Exception {
        Random rand = new Random(0x1234FFDA);
        MutableMultinomial<Integer> d1 = new MutableMultinomial<>(100);
        MutableMultinomial<Integer> d2 = new MutableMultinomial<>(100);
        for (int i = 0; i < 100; i++) {
            int val = rand.nextInt(10_000);
            int noise = rand.nextInt(10);
            d1.set(i, val);
            d2.set(i, val + noise);
        }
        d1.normalize();
        d2.normalize();
        double mineSays = d1.kullbackLieblerTo(d2);
        log.info("KL d1||d2 is " + mineSays);
        double[] d1d = new double[100];
        double[] d2d = new double[100];
        d1.fillDensity(Ordering.natural(), d1d);
        d2.fillDensity(Ordering.natural(), d2d);
        double malletSays = Maths.klDivergence(d1d, d2d);
        log.info("Mallet says " + malletSays);
        Assert.assertEquals(malletSays, mineSays, 0.0001);
    }

    @Test
    public void shouldJensonShannon() throws Exception {
        Random rand = new Random(0x1234FFDA);
        MutableMultinomial<Integer> d1 = new MutableMultinomial<>(100);
        MutableMultinomial<Integer> d2 = new MutableMultinomial<>(100);
        for (int i = 0; i < 100; i++) {
            int val = rand.nextInt(10_000);
            int noise = rand.nextInt(100);
            d1.set(i, val);
            d2.set(i, val + noise);
        }
        d1.normalize();
        d2.normalize();
        double mineSays = d1.jensonShannonDivergence(d2);
        log.info("JS d1||d2 is " + mineSays);
        double[] d1d = new double[100];
        double[] d2d = new double[100];
        d1.fillDensity(Ordering.natural(), d1d);
        d2.fillDensity(Ordering.natural(), d2d);
        double malletSays = Maths.jensenShannonDivergence(d1d, d2d);
        log.info("Mallet says " + malletSays);
        Assert.assertEquals(malletSays, mineSays, 0.001);
        Assert.assertEquals(mineSays, d2.jensonShannonDivergence(d1), 0.001);
    }

    @Test
    public void shouldIterateParallel() throws Exception {
        MutableMultinomial<Integer> multi = MutableMultinomial.createUnknownMax();
        double valSum = 0;
        long idxSum = 0;
        int count = 1_000_000;
        Well19937c rand = new Well19937c();
        for (int i = 0; i < count; i++) {
            idxSum += i;
            double thisCount = rand.nextInt(50_000);
            valSum += thisCount;
            multi.add(i, thisCount);
        }
        log.info("Inserted " + count + " with a random val sum of " + valSum + " and idx sum of " + idxSum);
        LongAdder idxSum2 = new LongAdder();
        DoubleAdder valSum2 = new DoubleAdder();

        Stream<Entry<Integer>> stream = multi.entries().parallelStream();
        assertTrue(stream.isParallel());
        long entryCount = stream
                .map(e -> {
                    idxSum2.add(e.getKey());
                    valSum2.add(e.getDoubleValue());
                    return e;
                })
                .count();

        Truth.assertThat(idxSum2.sum()).isEqualTo(idxSum);
        Truth.assertThat(valSum2.sum()).isWithin(0.001).of(valSum);
        Truth.assertThat(entryCount).isEqualTo(count);
        log.info("Got valsum " + valSum2.sum() + " and idx sum " + idxSum2.sum());
    }
}