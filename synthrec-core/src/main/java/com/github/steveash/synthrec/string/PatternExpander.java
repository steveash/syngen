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

import static com.github.steveash.synthrec.string.PatternReducer.END_ESC;
import static com.github.steveash.synthrec.string.PatternReducer.START_ESC;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.stat.RandUtil;

/**
 * Expands patterns (NOT prefixed, must be unprefixed already) and can optionally replace consecutive A's
 * with actual words if you supply a binned dictionary sampler
 * @author Steve Ash
 * @see PatternReducer
 */
public class PatternExpander {

    private final int minDictSamplerLength;
    @Nullable private final BinnedDictSampler dictSampler;

    public PatternExpander() {
        this(0, null);
    }

    /**
     * Creates an expander with the given dictSampler. minDictSamplerLength controls the
     * smallest length that we query from the dictionary. Anything smaller will be
     * randomly generated
     * @param minDictSamplerLength
     * @param dictSampler
     */
    public PatternExpander(int minDictSamplerLength, @Nullable BinnedDictSampler dictSampler) {
        this.minDictSamplerLength = minDictSamplerLength;
        this.dictSampler = dictSampler;
    }

    public String expandIfNeeded(RandomGenerator rand, String maybePattern) {
        if (PatternReducer.isTagged(maybePattern)) {
            return expand(rand, PatternReducer.unTag(maybePattern));
        }
        if (StringBinner.isTagged(maybePattern)) {
            return expand(rand, StringBinner.unTag(maybePattern));
        }
        return maybePattern;
    }

    public String expand(RandomGenerator rand, String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int consecutiveAs = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (PatternReducer.matchesAt(pattern, START_ESC, i)) {
                // found the start of an escaped section, find the end (note nesting)
                int end = findEndEscape(pattern, i);
                if (end >= 0) {
                    sb.append(pattern.substring(i + START_ESC.length(), end - END_ESC.length()));
                    i = end - 1; // because the fori is going to add one
                    continue;
                }
            }
            if (c == 'A') {
                consecutiveAs += 1;
                continue;
            } else {
                if (consecutiveAs > 0) {
                    // dump a word
                    sb.append(sampleWord(rand, consecutiveAs));
                    consecutiveAs = 0;
                }
            }
            if (c == '9') {
                if (i == 0 || i == pattern.length() - 1) {
                    // we record leading and trailing zeroes so only gen from the limited range
                    sb.append(1 + rand.nextInt(9));
                } else {
                    sb.append(rand.nextInt(10));
                }
                continue;
            }
            // punctuation, zeroes, just leave as is
            sb.append(c);
        }
        // might be buffered A's at the end
        if (consecutiveAs > 0) {
            // dump a word
            sb.append(sampleWord(rand, consecutiveAs));
            consecutiveAs = 0;
        }
        return sb.toString();
    }

    // takes the pattern and the index pointing to the first character of the escape sequence i.e. {{
    // and returns the index of the first character _after_ the end of the escape sequence
    private int findEndEscape(String pattern, int openEscIndex) {
        int pending = 1;
        for (int i = openEscIndex + START_ESC.length(); i < pattern.length(); i++) {
            if (PatternReducer.matchesAt(pattern, START_ESC, i)) {
                // found _another_ one
                pending += 1;
                i += START_ESC.length() - 1; // we're going to get 1 for free from the iteration
            } else if (PatternReducer.matchesAt(pattern, END_ESC, i)) {
                pending -= 1;
                if (pending == 0) {
                    return i + END_ESC.length();
                }
            }
        }
        return -1;
    }

    private String sampleWord(RandomGenerator rand, int length) {
        if (dictSampler == null || length < minDictSamplerLength) {
            return RandUtil.randomAlpha(rand, length);
        }
        return dictSampler.sample(length, rand);
    }
}
