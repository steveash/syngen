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

package com.github.steveash.synthrec.util;

import com.google.common.base.Preconditions;

/**
 * @author Steve Ash
 */
public class MoreMath {

    public static int minMax(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    public static long minMax(long value, long min, long max) {
        return Math.min(max, Math.max(min, value));
    }

    public static double minMax(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    public static double euclideanDistance(double[] vec1, double[] vec2) {
        Preconditions.checkArgument(vec1.length == vec2.length, "different dims");
        double sum = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            sum = Math.pow(vec1[i] - vec2[i], 2.0);
        }
        return Math.sqrt(sum);
    }

    public static double percSpreadOf(int value, int min, int max) {
        if (min == max) {
            return 0;
        }
        value = minMax(value, min, max);
        double spread = max - min;
        return ((double)(value - min)) / spread;
    }

    /**
     * Substract a - b where a and b are probabilities being expressed in log space
     * @param a
     * @param b
     * @return
     */
    public static double subtractLogProb(double a, double b) {
        if (b == Double.NEGATIVE_INFINITY)
            return a;
        else
            return a + Math.log(1 - Math.exp(b - a));
    }

    /**
     * Add a + b where a and b are probabilities being expressed in log space
     * @param a
     * @param b
     * @return
     */
    public static double sumLogProb(double a, double b) {
        if (a == Double.NEGATIVE_INFINITY)
            return b;
        else if (b == Double.NEGATIVE_INFINITY)
            return a;
        else if (b < a)
            return a + Math.log(1 + Math.exp(b - a));
        else
            return b + Math.log(1 + Math.exp(a - b));
    }
}
