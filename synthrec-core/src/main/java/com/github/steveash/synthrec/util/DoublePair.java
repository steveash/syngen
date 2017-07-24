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

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Pair of doubles (x,y)
 * @author Steve Ash
 */
public class DoublePair {

    /**
     * Produces a one-based (x,y) of index -> val
     * @param vals
     * @return
     */
    public static Iterable<DoublePair> ranked(DoubleArrayList vals) {
        return () -> new AbstractIterator<DoublePair>() {
            private int i = 0;
            @Override
            protected DoublePair computeNext() {
                if (i >= vals.size()) {
                    return endOfData();
                }
                DoublePair pair = new DoublePair(i + 1, vals.getDouble(i));
                i += 1;
                return pair;
            }
        };
    }

    private final double x;
    private final double y;

    public DoublePair(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoublePair that = (DoublePair) o;

        if (Double.compare(that.x, x) != 0) return false;
        return Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "DoublePair{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
