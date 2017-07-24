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

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

/**
 * @author Steve Ash
 */
public class RandUtil {

    private static ThreadLocalRandomGenerator LOCAL_WELL = new ThreadLocalRandomGenerator(Well19937c::new);

    public static ThreadLocalRandomGenerator threadLocalRand() {
        return LOCAL_WELL;
    }

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static int nextIntIn(RandomGenerator rand, int minIncl, int maxExcl) {
        return minIncl + rand.nextInt(maxExcl - minIncl);
    }

    public static String randomAlpha(RandomGenerator rand, int length) {
        if (length == 1) {
            int nextIdx = rand.nextInt(LETTERS.length());
            return LETTERS.substring(nextIdx, nextIdx + 1);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int nextIdx = rand.nextInt(LETTERS.length());
            char c = LETTERS.charAt(nextIdx);
            sb.append(c);
        }
        return sb.toString();
    }
}
