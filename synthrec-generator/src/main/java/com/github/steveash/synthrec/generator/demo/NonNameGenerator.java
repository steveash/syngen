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

package com.github.steveash.synthrec.generator.demo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.canonical.Normalizers;
import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.gen.TooManyRejectsSamplingException;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.CensusGivenNames;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Generator for sequences of non-name tokens
 * The strategy is really naive right now, but you could imagine this doing something more
 * sophisticated in the future
 * @author Steve Ash
 */
@LazyComponent
public class NonNameGenerator {
    private static final Logger log = LoggerFactory.getLogger(NonNameGenerator.class);

    private static final int MAX_TRIES = 10_000;
    private final Sampler<String> words;

    @Autowired
    public NonNameGenerator(CensusGivenNames censusGivenNames) {
        MutableMultinomial<String> multi = MutableMultinomial.createUnknownMax();
        emit(multi, censusGivenNames, "words/google-10000-english-usa-no-swears-medium.clob", 5_000);
        emit(multi, censusGivenNames, "words/google-10000-english-usa-no-swears-short.clob", 5_000);
        emit(multi, censusGivenNames, "words/google-10000-english-usa-no-swears-long.clob", 3_000);
        emit(multi, censusGivenNames, "words/known-abbrev.clob", 4_000);
        log.info("Using dictionary of " + multi.size() + " entries for non-name phrase generation");
        words = SamplingTable.createFromMultinomial(multi);
    }

    private void emit(MutableMultinomial<String> multi, CensusGivenNames names, String file, final int start) {
        int count = start;
        int rowNumber = 0;
        int strideWidth = 1; // each time increase string by this much
        final int strideIncrement = 3; // each time increase string by this much
        int nextStrideRowNumber = strideWidth; // starting spot
        final int countDecrement = 10; // each stride we decrease by this amount

        CsvTable table = CsvTable.loadSource(ReadWrite.findResource(file))
                .noHeaders(1)
                .trimResults()
                .build();
        for (Row row : table) {
            String value = Names.normalize(row.getString(0));
            if (names.isInCensusNames(value)) {
                continue;
            }
            rowNumber += 1;
            multi.add(Normalizers.interner().intern(value), count);
            if (rowNumber >= nextStrideRowNumber) {
                nextStrideRowNumber += strideWidth;
                strideWidth += strideIncrement;
                count = Math.max(1, count - countDecrement);
            }
        }
    }

    public List<String> phrase(RandomGenerator rand, int count) {
        if (count == 1) {
            return ImmutableList.of(words.sample(rand));
        }
        String first = words.sample(rand);
        if (count == 2) {
            for (int i = 0; i < MAX_TRIES; i++) {
                String second = words.sample(rand);
                if (!first.equalsIgnoreCase(second)) {
                    return ImmutableList.of(first, second);
                }
            }
            throw new TooManyRejectsSamplingException();
        }
        HashSet<String> seen = Sets.newHashSetWithExpectedSize(count);
        ArrayList<String> result = Lists.newArrayListWithCapacity(count);
        seen.add(first);
        result.add(first);
        for (int i = 0; i < MAX_TRIES; i++) {
            String next = words.sample(rand);
            if (!seen.add(next)) {
                // already seen
                continue;
            }
            result.add(next);
            if (result.size() == count) {
                return result;
            }
        }
        throw new TooManyRejectsSamplingException();
    }
}
