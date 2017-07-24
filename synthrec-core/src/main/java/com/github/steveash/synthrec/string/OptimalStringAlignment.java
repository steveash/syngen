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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.primitives.Shorts.checkedCast;
import static java.lang.Math.abs;
import static java.lang.Math.max;

import java.util.Arrays;

import com.google.common.annotations.VisibleForTesting;

/**
 * Implementation of the OSA which is similar to the Damerau-Levenshtein in that it allows for transpositions to
 * count as a single edit distance, but is not a true metric and can over-estimate the cost because it disallows
 * substrings to edited more than once.  See wikipedia for more discussion on OSA vs DL
 * <p/>
 * See Algorithms on Strings, Trees and Sequences by Dan Gusfield for more information.
 * <p/>
 * This also has a set of local buffer implementations to avoid allocating new buffers each time, which might be
 * a premature optimization
 * <p/>
 * @author Steve Ash
 */
public class OptimalStringAlignment {

    private static final int threadLocalBufferSize = 64;

    private static final ThreadLocal<short[]> costLocal = new ThreadLocal<short[]>() {
        @Override
        protected short[] initialValue() {
            return new short[threadLocalBufferSize];
        }
    };

    private static final ThreadLocal<short[]> back1Local = new ThreadLocal<short[]>() {
        @Override
        protected short[] initialValue() {
            return new short[threadLocalBufferSize];
        }
    };

    private static final ThreadLocal<short[]> back2Local = new ThreadLocal<short[]>() {
        @Override
        protected short[] initialValue() {
            return new short[threadLocalBufferSize];
        }
    };

    public static double editDistanceNormalzied(String a, String b) {
        int max = Math.max(a.length(), b.length());
        int edits = OptimalStringAlignment.editDistance(a, b, max + 1);
        return ((double) edits) / ((double) max);
    }

    public static int editDistance(CharSequence s, CharSequence t, int threshold) {
        checkNotNull(s, "cannot measure null strings");
        checkNotNull(t, "cannot measure null strings");
        checkArgument(threshold >= 0, "Threshold must not be negative");
        checkArgument(s.length() < Short.MAX_VALUE, "Cannot take edit distance of strings longer than 32k chars");
        checkArgument(t.length() < Short.MAX_VALUE, "Cannot take edit distance of strings longer than 32k chars");

        if (s.length() + 1 > threadLocalBufferSize || t.length() + 1 > threadLocalBufferSize)
            return editDistanceWithNewBuffers(s, t, checkedCast(threshold));

        short[] cost = costLocal.get();
        short[] back1 = back1Local.get();
        short[] back2 = back2Local.get();
        return editDistanceWithBuffers(s, t, checkedCast(threshold), back2, back1, cost);
    }

    @VisibleForTesting
    static int editDistanceWithNewBuffers(CharSequence s, CharSequence t, short threshold) {
        int slen = s.length();
        short[] back1 = new short[slen + 1];    // "up 1" row in table
        short[] back2 = new short[slen + 1];    // "up 2" row in table
        short[] cost = new short[slen + 1];     // "current cost"

        return editDistanceWithBuffers(s, t, threshold, back2, back1, cost);
    }

    private static int editDistanceWithBuffers(CharSequence s, CharSequence t, short threshold,
            short[] back2, short[] back1, short[] cost
    ) {

        short slen = (short) s.length();
        short tlen = (short) t.length();

        // if one string is empty, the edit distance is necessarily the length of the other
        if (slen == 0) {
            return tlen <= threshold ? tlen : -1;
        } else if (tlen == 0) {
            return slen <= threshold ? slen : -1;
        }

        short delta = (short) abs(slen - tlen);

        // if lengths are different > k, then can't be within edit distance
        if (delta > threshold)
            return -1;

        short lowerStripWidth = (short) ((threshold + delta) / 2);
        short upperStripWidth = (short) ((threshold - delta) / 2);

        if (slen > tlen) {
            // swap the two strings to consume less memory
            CharSequence tmp = s;
            s = t;
            t = tmp;
            slen = tlen;
            tlen = (short) t.length();
        }

        initMemoiseTables(threshold, back2, back1, cost, slen);

        for (short j = 1; j <= tlen; j++) {
            cost[0] = j; // j is the cost of inserting this many characters

            // stripe bounds
            int min = max(1, j - lowerStripWidth);
            int max = min(slen, (short) (j + upperStripWidth));

            // at this iteration the left most entry is "too much" so reset it
            if (min > 1) {
                cost[min - 1] = Short.MAX_VALUE;
            }

            short minCostInStripe = iterateOverStripe(s, t, j, cost, back1, back2, min, max);
            if (minCostInStripe > threshold) {
                return -1;  // early terminate if you've already exceeded the cost
            }

            // swap our cost arrays to move on to the next "row"
            short[] tempCost = back2;
            back2 = back1;
            back1 = cost;
            cost = tempCost;
        }

        // after exit, the current cost is in back1
        // if back1[slen] > k then we exceeded, so return -1
        if (back1[slen] > threshold) {
            return -1;
        }
        return back1[slen];
    }

    /**
     * Iterate over the stripe in [min,max] and fill in the cost array'
     */
    private static short iterateOverStripe(CharSequence s, CharSequence t, short j,
            short[] cost, short[] back1, short[] back2, int min, int max
    ) {

        short minCostInStripe = Short.MAX_VALUE;
        // iterates over the stripe
        for (int i = min; i <= max; i++) {

            if (s.charAt(i - 1) == t.charAt(j - 1)) {
                cost[i] = back1[i - 1];
            } else {
                cost[i] = (short) (1 + min(cost[i - 1], back1[i], back1[i - 1]));
            }
            if (i >= 2 && j >= 2) {
                // possible transposition to check for
                if ((s.charAt(i - 2) == t.charAt(j - 1)) &&
                        s.charAt(i - 1) == t.charAt(j - 2)) {
                    cost[i] = min(cost[i], (short) (back2[i - 2] + 1));
                }
            }
            if (cost[i] < minCostInStripe)
                minCostInStripe = cost[i];
        }
        return minCostInStripe;
    }

    private static void initMemoiseTables(short threshold, short[] back2, short[] back1, short[] cost, short slen) {
        // initial "starting" values for inserting all the letters
        short boundary = (short) (min(slen, threshold) + 1);
        for (short i = 0; i < boundary; i++) {
            back1[i] = i;
            back2[i] = i;
        }
        // need to make sure that we don't read a default value when looking "up"
        Arrays.fill(back1, boundary, slen + 1, Short.MAX_VALUE);
        Arrays.fill(back2, boundary, slen + 1, Short.MAX_VALUE);
        Arrays.fill(cost, 0, slen + 1, Short.MAX_VALUE);
    }

    private static short min(short a, short b) {
        return (a <= b ? a : b);
    }

    private static short min(short a, short b, short c) {
        return min(a, min(b, c));
    }
}