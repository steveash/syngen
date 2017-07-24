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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.guavate.Guavate;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.util.MoreCollections;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.primitives.Doubles;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

/**
 * A class that represents a categorical frequency distribution to sample from
 * the values here are cumulative (negative log(prob)) (for precision)
 * @author Steve Ash
 */
public class SamplingTable<T> implements Sampler<T> {

    public static <T> SamplingTable<T> createFromNormalized(Multinomial<T> density) {
        Preconditions.checkArgument(density.isNormalized(), "must pass a normalized distribution", density);
        int count = 0;
        for (Object2DoubleMap.Entry<T> entry : density.entries()) {
            if (entry.getDoubleValue() > 0) {
                count += 1;
            }
        }
        double cum = 0;
        int i = 0;
        Object[] values = new Object[count];
        double[] cumLogProb = new double[count];
        for (Object2DoubleMap.Entry<T> entry : density.entries()) {
            double dv = entry.getDoubleValue();
            if (dv > 0) {
                values[i] = Preconditions.checkNotNull(entry.getKey(), "cant pass null values", density);
                Preconditions.checkArgument(Doubles.isFinite(dv), "NaN/inf in density", density);
                cum += dv;
                double log = Math.log(cum);
                Preconditions.checkArgument(Doubles.isFinite(log), "infinite log prob", density, cum);
                cumLogProb[i] = log;
                i += 1;
            }
        }
        return new SamplingTable<T>(values, cumLogProb);
    }

    public static <T> Map<AssignmentInstance, SamplingTable<T>> createConditionalFromMultinomial(Map<AssignmentInstance, ? extends Multinomial<T>> conditional) {
        return Maps.newHashMap(Maps.transformValues(conditional, SamplingTable::createFromMultinomial));
    }

    public static <T> SamplingTable<T> createFromMultinomial(Multinomial<T> density) {
        int count = 0;
        double sum = 0;
        for (Object2DoubleMap.Entry<T> entry : density.entries()) {
            if (entry.getDoubleValue() > 0) {
                count += 1;
                sum += entry.getDoubleValue();
            }
        }
        Preconditions.checkState(sum > 0, "cant smaple from empty multinomial");
        double totalCount = 0;
        int i = 0;
        double logSum = Math.log(sum);
        Object[] values = new Object[count];
        double[] cumLogProb = new double[count];
        for (Object2DoubleMap.Entry<T> entry : density.entries()) {
            double dv = entry.getDoubleValue();
            if (dv > 0) {
                values[i] = Preconditions.checkNotNull(entry.getKey(), "cant pass null values", density);
                Preconditions.checkArgument(Doubles.isFinite(dv), "NaN/inf in density", density);
                totalCount += dv;
                double logCum = Math.log(totalCount) - logSum;
                Preconditions.checkArgument(Doubles.isFinite(logCum), "infinite log prob", density, logCum);
                cumLogProb[i] = logCum;
                i += 1;
            }
        }
        return new SamplingTable<T>(values, cumLogProb);
    }

    public static <T> SamplingTable<T> createFromCountEntries(Iterable<? extends Entry<T>> entries) {
        int count = 0;
        long sum = 0;
        for (Entry<T> entry : entries) {
            Preconditions.checkArgument(entry.getIntValue() > 0, "cant pass 0 entries");
            count += 1;
            sum += entry.getIntValue();
        }
        long cum = 0;
        int i = 0;
        Object[] values = new Object[count];
        double[] cumLogProb = new double[count];
        double denom = Math.log(sum);
        for (Entry<T> entry : entries) {
            values[i] = Preconditions.checkNotNull(entry.getKey(), "cant pass null values", entries);
            cum += entry.getIntValue();
            cumLogProb[i] = Math.log(cum) - denom;
            i += 1;
        }
        return new SamplingTable<T>(values, cumLogProb);
    }

    private final Object[] values;
    private final double[] cumLogProb;

    // create from a normalized probability distribution
    private SamplingTable(Object[] values, double[] cumLogProb) {
        this.values = values;
        this.cumLogProb = cumLogProb;
    }

    public T sampleUniform(RandomGenerator rand) {
        return (T) values[rand.nextInt(values.length)];
    }

    public T sampleWeighted(RandomGenerator rand) {
        double nextValue = Math.log(rand.nextDouble());
        int found = Arrays.binarySearch(cumLogProb, nextValue);
        if (found >= 0) {
            // by magic we hit the exact value
            return (T) values[found];
        }
        // binary search returns (-insertPlace) - 1 so recover insert spot, thats our sample
        int insert = -(found + 1);
        Preconditions.checkState(insert >= 0 && insert <= values.length);
        if (insert >= values.length) {
            return (T) values[values.length - 1];
        }
        return (T) values[insert];
    }

    /**
     * Adapter for the Sampler interface, returns sampleWeighted()
     * @param rand
     * @return
     */
    @Override
    public T sample(RandomGenerator rand) {
        return sampleWeighted(rand);
    }
}
