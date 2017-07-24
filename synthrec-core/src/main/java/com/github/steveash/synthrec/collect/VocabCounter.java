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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;

import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.collect.Iterators;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Efficiently counts the occurences of a vocab
 * <p>
 * Notes:
 * int2dbl records the vocab idx -> count (which might be partial due to uncertain assignments)
 * vocab references the parent vocabulary
 * @author Steve Ash
 */
public class VocabCounter<T> implements Serializable {

    private static final long serialVersionUID = -1019438421328720060L;

    private final Int2DoubleOpenHashMap counts = new Int2DoubleOpenHashMap();
    private final Vocabulary<T> vocab;

    public VocabCounter(Vocabulary<T> vocab) {this.vocab = vocab;}

    public Vocabulary<T> getVocab() {
        return vocab;
    }

    public void incrementByIndex(int valueIndex) {
        addByIndex(valueIndex, 1);
    }

    public void incrementByValue(T value) {
        incrementByIndex(vocab.putIfAbsent(value));
    }

    public void addByIndex(int valueIndex, double countToAdd) {
        counts.addTo(valueIndex, countToAdd);
    }

    public void addByValue(T value, double countToAdd) {
        addByIndex(vocab.putIfAbsent(value), countToAdd);
    }

    public double countByIndex(int valueIndex) {
        return counts.get(valueIndex);
    }

    public int entryCount() {
        return counts.size();
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public double countByValue(T value) {
        int idx = vocab.tryGetIndexFor(value);
        if (idx <= 0) {
            return 0;
        }
        return countByIndex(idx);
    }

    public MutableMultinomial<T> convertToMultinomial() {
        MutableMultinomial<T> multi = new MutableMultinomial<>(counts.size());
        ObjectIterator<Entry> iter = counts.int2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry next = iter.next();
            T value = vocab.getForIndex(next.getIntKey());
            multi.set(value, next.getDoubleValue());
        }
        return multi;
    }

    public void printTo(PrintWriter pw) {
        convertToMultinomial().printTo(pw);
    }

    public Iterator<Object2DoubleMap.Entry<T>> fastIterator() {
        ObjectIterator<Entry> iter = counts.int2DoubleEntrySet().fastIterator();

        final IterEntry<T> entry = new IterEntry<T>();
        return Iterators.transform(iter, e -> {
            T val = vocab.getForIndex(e.getIntKey());
            entry.setKey(val);
            entry.setValue(e.getDoubleValue());
            return entry;
        });
    }

    private static class IterEntry<T> implements Object2DoubleMap.Entry<T> {

        private T key;
        private double val;

        @Override
        public T getKey() {
            return key;
        }

        public void setKey(T key) {
            this.key = key;
        }

        @Override
        public Double getValue() {
            return val;
        }

        @Override
        public Double setValue(Double value) {
            double oldVal = val;
            this.val = value;
            return oldVal;
        }

        @Override
        public double setValue(double value) {
            double oldVal = val;
            this.val = value;
            return oldVal;
        }

        @Override
        public double getDoubleValue() {
            return val;
        }
    }
}
