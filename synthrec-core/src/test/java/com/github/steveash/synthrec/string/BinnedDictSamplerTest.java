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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.random.Well19937c;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.SampleEntry;
import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
public class BinnedDictSamplerTest {
    private static final Logger log = LoggerFactory.getLogger(BinnedDictSamplerTest.class);

    @Test
    public void shouldSampleSimply() throws Exception {
        Well19937c rand = new Well19937c();
        BinnedDictSampler sampler = makeSampleBinnedDict();
        for (int i = 2; i < 40; i++) {
            for (int j = 0; j < 3; j++) {

                String sampled = sampler.sample(i, rand);
                log.info("For len " + i + " sampled " + sampled);
                if (i == 8) {
                    Assert.assertTrue(sampled.length() != i);
                } else {
                    Assert.assertTrue(sampled.length() >= i - 10 && sampled.length() <= i + 10);
                }
            }
        }
    }

    public static BinnedDictSampler makeSampleBinnedDict() {
        List<String> allWords = Lists.newArrayList();
        for (int i = 2; i < 40; i++) {
            if (i == 8) continue; // nothing length 8

            for (int j = 0; j < 100; j++) {
                String val = String.format("%02d", j);
                val += StringUtils.repeat("Z", i - val.length());
                allWords.add(val);
            }
        }
        Iterable<SampleEntry<String>> ible = allWords.stream()
                .map(s -> new SampleEntry<>(s, 1.0))
                ::iterator;

        return new BinnedDictSampler(ible);
    }
}