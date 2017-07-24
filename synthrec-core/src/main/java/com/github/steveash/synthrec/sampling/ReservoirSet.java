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

package com.github.steveash.synthrec.sampling;

import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A set of T's that has a max size and applies reservoir sampling after the max size to get a sample
 * ThreadSafe
 * @author Steve Ash
 */
public class ReservoirSet<T> {

    private final List<T> list;
    private final Set<T> set;
    private final int maxSize;
    private int totalTried = 0;

    public ReservoirSet(int maxSize) {
        this.maxSize = maxSize;
        this.list = Lists.newArrayList();
        this.set = Sets.newHashSet();
    }

    public synchronized void tryAdd(RandomGenerator rand, T item) {
        if (set.contains(item)) {
            return;
        }
        totalTried += 1;
        int currentSize = list.size();
        if (currentSize < maxSize) {
            list.add(item);
            set.add(item);
            return;
        }
        int sample = rand.nextInt(totalTried + 1);
        if (sample < currentSize) {
            T replaced = list.get(sample);
            list.set(sample, item);
            set.remove(replaced);
            set.add(item);
        }
    }

    public synchronized int getTotalTried() {
        return totalTried;
    }

    public synchronized Set<T> getFinalSet() {
        return set;
    }
}
