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

package com.github.steveash.synthrec.string;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.gen.TooManyRejectsSamplingException;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * A type that allows you to sample for words by a particular length (or at least binned to a particular length
 * meaning that you might ask for a 9 char word and get a 10). There will be some variance in the lengths that
 * is correlated to their lengths.
 * @author Steve Ash
 */
public class BinnedDictSampler {

    private static final int MAX_ATTEMPTS = 1_000;
    private static final int MIN_COUNT = 2;

    private static final int[] BINS = new int[] {
            2, 3, 4, 5, 6, 7, 8, 9,
            10,
            13,
            16,
            20,
            24,
            28,
            35,
            42
    };

    private final List<Sampler<String>> binnedSamplers;

    public BinnedDictSampler(Iterable<? extends Object2DoubleMap.Entry<String>> entries) {
        this(entries, s -> s);
    }

    public BinnedDictSampler(Iterable<? extends Object2DoubleMap.Entry<String>> entries, Function<String,String> normalizer) {
        List<MutableMultinomial<String>> binned = Lists.newArrayListWithCapacity(BINS.length + 1);
        // one extra for things longer than the biggest bin
        for (int i = 0; i < BINS.length + 1; i++) {
            binned.add(new MutableMultinomial<>(-1));
        }
        for (Entry<String> entry : entries) {
            int idx = binIndex(entry.getKey());
            binned.get(idx).add(normalizer.apply(entry.getKey()), entry.getDoubleValue());
        }
        List<Sampler<String>> builder = Lists.newArrayListWithCapacity(BINS.length + 1);
        for (MutableMultinomial<String> multi : binned) {
            if (multi.size() > MIN_COUNT) {
                builder.add(SamplingTable.createFromMultinomial(multi));
            } else {
                builder.add(null); // place holder for "try somewhere else"
            }
        }
        this.binnedSamplers = builder;
    }

    private static int binIndex(String word) {
        int len = word.length();
        return binIndex(len);
    }

    private static int binIndex(int len) {
        for (int i = 0; i < BINS.length; i++) {
            if (len <= BINS[i]) {
                return i;
            }
        }
        return BINS.length;
    }

    public String sample(int wantedLength, RandomGenerator rand) {
        int idx = binIndex(wantedLength);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            Sampler<String> sampler = binnedSamplers.get(idx);
            if (sampler != null) {
                return sampler.sample(rand);
            }
            idx = rand.nextInt(binnedSamplers.size());
        }
        throw new TooManyRejectsSamplingException();
    }
}
