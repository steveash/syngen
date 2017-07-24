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

import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Lists;
import com.google.common.math.IntMath;
import com.google.common.util.concurrent.Striped;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * A concurrent counting structure that helps count big multinomials in memory and can output a
 * real multinomial; will need enough space for O(2N) to actually build multinomial
 * @author Steve Ash
 */
public class ConcurrentCounter<T> {

    private static final int STRIPE_COUNT = 500;
    private static final int MASK = ceilToPowerOfTwo(STRIPE_COUNT) - 1;
    private static final int EXPECTED_PER_STRIPE = 2_000;

    private static int ceilToPowerOfTwo(int x) {
        return 1 << IntMath.log2(x, RoundingMode.CEILING);
    }

    private final Striped<Lock> stripes = Striped.lock(STRIPE_COUNT);
    private final List<Object2DoubleOpenHashMap<T>> maps;

    public ConcurrentCounter() {
        this.maps = Lists.newArrayListWithCapacity(MASK + 1); // the cells of this map are guarded by stripe
        for (int i = 0; i < MASK + 1; i++) {
            maps.add(null);
        }
    }

    public void increment(T value) {
        add(value, 1.0);
    }

    public void add(T value, double count) {
        Lock lock = stripes.get(value);
        lock.lock();
        try {
            int index = getIndexFor(value);
            Object2DoubleOpenHashMap<T> map = maps.get(index);
            if (map == null) {
                map = new Object2DoubleOpenHashMap<>(EXPECTED_PER_STRIPE);
                maps.set(index, map);
            }
            map.addTo(value, count);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        for (int i = 0; i < stripes.size(); i++) {
            Lock stripe = stripes.getAt(i);
            try {
                stripe.lock();
                // the visibility of this relies on my index method being the same as guavas
                maps.set(i, null);
            } finally {
                stripe.unlock();
            }
        }
    }

    public int size() {
        int count = 0;
        for (int i = 0; i < stripes.size(); i++) {
            Lock stripe = stripes.getAt(i);
            try {
                stripe.lock();
                // the visibility of this relies on my index method being the same as guavas
                Object2DoubleOpenHashMap<T> maybe = maps.get(i);
                if (maybe != null) {
                    count += maybe.size();
                }
            } finally {
                stripe.unlock();
            }
        }
        return count;
    }

    public MutableMultinomial<T> drainTo() {
        MutableMultinomial<T> multi = MutableMultinomial.createUnknownMax(size());
        for (int i = 0; i < stripes.size(); i++) {
            Lock stripe = stripes.getAt(i);
            try {
                stripe.lock();
                // the visibility of this relies on my index method being the same as guavas
                Object2DoubleOpenHashMap<T> maybe = maps.get(i);
                if (maybe != null) {
                    multi.distrib.putAll(maybe);
                }
            } finally {
                stripe.unlock();
            }
        }
        return multi;
    }

    private int getIndexFor(T value) {
        int hash = smear(value.hashCode());
        return hash & MASK;
    }

    // Copied from java/com/google/common/collect/Hashing.java
    private static int smear(int hashCode) {
        hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
        return hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
    }
}
