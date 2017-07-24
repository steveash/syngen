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

import com.google.common.collect.Ordering;

/**
 * @author Steve Ash
 */
public class Object2Double<T> {

    public static <T extends Comparable<T>> Ordering<Object2Double<T>> orderByKey() {
        return new Ordering<Object2Double<T>>() {
            @Override
            public int compare(Object2Double<T> left, Object2Double<T> right) {
                return Ordering.natural().compare(left.getKey(), right.getKey());
            }
        };
    }

    public static <T> Ordering<Object2Double<T>> orderByValue() {
        return new Ordering<Object2Double<T>>() {
            @Override
            public int compare(Object2Double<T> left, Object2Double<T> right
            ) {
                return Double.compare(left.getValue(), right.getValue());
            }
        };
    }

    public static <T> Object2Double<T> of(T key, double val) {
        return new Object2Double<T>(key, val);
    }

    private final T key;
    private final double val;

    public Object2Double(T key, double val) {
        this.key = key;
        this.val = val;
    }

    public T getKey() {
        return key;
    }

    public double getValue() {
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Object2Double<?> that = (Object2Double<?>) o;

        if (Double.compare(that.val, val) != 0) return false;
        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = key != null ? key.hashCode() : 0;
        temp = Double.doubleToLongBits(val);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Object2Double{" +
                "key=" + key +
                ", val=" + val +
                '}';
    }
}
