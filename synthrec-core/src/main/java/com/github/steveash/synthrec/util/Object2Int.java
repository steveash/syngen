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
public class Object2Int<T> {

    public static <T extends Comparable<T>> Ordering<Object2Int<T>> orderByKey() {
        return new Ordering<Object2Int<T>>() {
            @Override
            public int compare(Object2Int<T> left, Object2Int<T> right) {
                return Ordering.natural().compare(left.getKey(), right.getKey());
            }
        };
    }

    public static <T> Ordering<Object2Int<T>> orderByValue() {
        return new Ordering<Object2Int<T>>() {
            @Override
            public int compare(Object2Int<T> left, Object2Int<T> right
            ) {
                return Integer.compare(left.getValue(), right.getValue());
            }
        };
    }

    public static <T> Object2Int<T> of(T key, int val) {
        return new Object2Int<T>(key, val);
    }

    private final T key;
    private final int val;

    public Object2Int(T key, int val) {
        this.key = key;
        this.val = val;
    }

    public T getKey() {
        return key;
    }

    public int getValue() {
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Object2Int<?> that = (Object2Int<?>) o;

        if (val != that.val) return false;
        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + val;
        return result;
    }

    @Override
    public String toString() {
        return "Object2Int{" +
                "key=" + key +
                ", val=" + val +
                '}';
    }
}
