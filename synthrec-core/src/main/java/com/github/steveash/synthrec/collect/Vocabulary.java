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

package com.github.steveash.synthrec.collect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * A vocabulary is a mapping of elements T to int indexes that supports fast mapping in both directions (value to int)
 * and int to value
 * Implementation notes:
 * - no mapping at index 0 (to avoid confusion since 0 is the default value for alot of these primitive lists)
 * - indexToValue might have nulls in it to indicate no mapping (or an updated mapping). These will be skipped by
 * an iterator going over this
 * - there are thombstone records like (fromIdx, toIdx) that record when two entries have been merged the
 * old index is no longer valid and wont be included in iteration -- but if you ask for the value by
 * the old index, you will get the value pointed to by the new index.  Since part of the point of a
 * vocabulary is to kind of immutably map things we have to always be able to answer even old queries
 * backwards compatibly
 * @author Steve Ash
 */
@NotThreadSafe
public class Vocabulary<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = -3522063725938515747L;

    private final Int2IntOpenHashMap tombstones = new Int2IntOpenHashMap();
    // id -> value, can contain nulls for redirects
    private final ArrayList<T> indexToValue = Lists.newArrayList();
    // value -> id, best representation of the real live vocab data (i.e. for size() which might differ from index2val)
    private final Object2IntOpenHashMap<T> valueToIndex = new Object2IntOpenHashMap<T>();

    {
        // dont store stuff at zero
        indexToValue.add(null);
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Vocabulary() {
    }

    public Vocabulary(Vocabulary<T> copyFrom) {
        indexToValue.clear();
        valueToIndex.clear();
        indexToValue.addAll(copyFrom.indexToValue);
        valueToIndex.putAll(copyFrom.valueToIndex);
        tombstones.putAll(copyFrom.tombstones);
    }

    /**
     * NOTE You can't iterate over this concurrently and mutate it -- the lock isn't held while you iterate
     * and the iterator isn't concurrent or safe
     * @return
     */
    @Override
    public Iterator<T> iterator() {
        lock.readLock().lock();
        try {
            if (valueToIndex.isEmpty()) {
                return Collections.emptyIterator();
            }
            // first element is a null placeholder
            return Iterators.filter(indexToValue.subList(1, indexToValue.size()).iterator(), Objects::nonNull);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the item from the vocabulary for this index but does not try to resolve any tombstone
     * records
     * @param index
     * @return
     */
    public T getForIndexNoResolve(int index) {
        lock.readLock().lock();
        try {
            if (index <= 0 || index > (indexToValue.size() - 1)) {
                throw new IllegalArgumentException("No index exists " + index);
            }
            return indexToValue.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the value for the given index
     * @param index
     * @return the value or null if it has been removed and not redirected (not sure if thats possible in current code)
     */
    public T getForIndex(int index) {
        lock.readLock().lock();
        try {
            if (index <= 0 || index > (indexToValue.size() - 1)) {
                throw new IllegalArgumentException("No index exists " + index);
            }
            T maybe = indexToValue.get(index);
            if (maybe == null) {
                int maybeRedirect = tombstones.get(index);
                if (maybeRedirect != 0) {
                    return getForIndex(maybeRedirect);
                }
            }
            return maybe;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * if this value is absent then this adds it and returns the resulting index for that item; otherwise it just
     * returns the index for the existing value mapping
     * @param value
     * @return
     */
    public int putIfAbsent(T value) {
        lock.writeLock().lock();
        try {
            Preconditions.checkNotNull(value, "cant insert null in vocab");
            if (valueToIndex.containsKey(value)) {
                return valueToIndex.getInt(value);
            }
            Preconditions.checkArgument(value instanceof Serializable, "not serializable", value.getClass(), value);
            indexToValue.add(value);
            int newId = indexToValue.size() - 1;
            valueToIndex.put(value, newId);
            return newId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int nextIndex() {
        lock.readLock().lock();
        try {
            return indexToValue.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the index for the given value assuming that the value has already been inserted; throws an exception
     * if the value doesn't exist
     * @param value
     * @return
     */
    public int getIndexFor(T value) {
        lock.readLock().lock();
        ;
        try {
            int idx = valueToIndex.getInt(value);
            if (idx <= 0) {
                throw new IllegalArgumentException("No mapping exists for value " + value);
            }
            return idx;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the index if it exists, or return -1 if not (getIndexFor throws if missing)
     * @param value
     * @return
     */
    public int tryGetIndexFor(T value) {
        lock.readLock().lock();
        try {
            int idx = valueToIndex.getInt(value);
            if (idx <= 0) {
                return -1;
            }
            return idx;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * If you want to change the index value to something else (like in a deidentifying procedure)
     * @param index
     * @param newValue
     * @return new index which is the same as index if the newValue is also new, but is going to be diff if this caused merge
     */
    public int updateIndexValue(int index, T newValue) {
        lock.writeLock().lock();
        try {
            Preconditions.checkNotNull(newValue, "cant insert null in vocab");
            Preconditions.checkArgument(newValue instanceof Serializable,
                    "not serializable",
                    newValue.getClass(),
                    newValue
            );
            if (valueToIndex.containsKey(newValue)) {
                // this is a remapping that will merge into another cell, so we will have a tombstone
                T oldValue = indexToValue.get(index);
                Preconditions.checkNotNull(oldValue, "cant update a retired/tombstoned index", index);
                int targetId = valueToIndex.getInt(newValue);
                if (targetId == index) {
                    return index;
                }
                tombstones.put(index, targetId);
                indexToValue.set(index, null); // we're tombstoning it
                valueToIndex.removeInt(oldValue);
                return targetId;
            } else {
                T oldValue = indexToValue.set(index, newValue);
                Preconditions.checkState(oldValue != null, "somehow a null value is in here");
                valueToIndex.removeInt(oldValue);
                valueToIndex.put(newValue, index);
                return index;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean contains(T value) {
        lock.readLock().lock();
        try {
            return valueToIndex.containsKey(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return valueToIndex.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    public void printTo(Consumer<String> logSink) {
        lock.readLock().lock();
        try {
            logSink.accept("Vocab{size=" + size());
            for (int i = 1; i < indexToValue.size(); i++) {
                T maybe = indexToValue.get(i);
                if (maybe != null) {
                    logSink.accept("  " + i + "->" + maybe);
                }
            }
            logSink.accept("}");
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean equalTo(Vocabulary<T> other) {
        lock.readLock().lock();
        try {
            other.lock.readLock().lock();
            try {
                // equality is only based on the live set -- not the datastructures.  Semantically if you have
                // a vocab that had some redirects -- and after the redirects it is equivalent to another with
                // no redirects, then they are still semantically representing ids to the same set with the
                // same numbers so should be equalTo()
                if (this.valueToIndex.size() != other.valueToIndex.size()) {
                    return false;
                }
                return valueToIndex.equals(other.valueToIndex);
            } finally {
                other.lock.readLock().unlock();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "Vocabulary{size=" + size() + "}";
    }
}
