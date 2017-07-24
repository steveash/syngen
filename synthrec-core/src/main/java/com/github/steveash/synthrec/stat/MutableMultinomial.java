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

import java.io.Serializable;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Represents a discrete probability distribution (or multinomial distribution) (or a builder to make one);
 * note that all "add" related methods just add values and do not normalize the distribution;
 * so its only actually a proability distribution after you call normalize() (going from a multinomial
 * distribution to a categorical distribution)
 * @author Steve Ash
 */
public class MutableMultinomial<T> extends Multinomial<T> implements Serializable {

    private static final long serialVersionUID = -2386784566082595284L;

    public static <T> MutableMultinomial<T> copyFrom(Multinomial<T> copyFrom) {
        MutableMultinomial<T> copy = new MutableMultinomial<>(copyFrom.maxEntries(), copyFrom.size());
        copy.addMultinomial(copyFrom);
        return copy;
    }

    public static <T> MutableMultinomial<T> createUnknownMax() {
        return new MutableMultinomial<>(-1);
    }

    public static <T> MutableMultinomial<T> createUnknownMax(int expectedCount) {
        return new MutableMultinomial<>(-1, expectedCount);
    }

    /**
     * If you pass -1 in here then when you normalize it it, it will update the max entries to the current size of the
     * observed discrete distribution
     * @param maxEntries
     */
    public MutableMultinomial(int maxEntries) {
        super(maxEntries);
    }

    protected MutableMultinomial(int maxEntries, int expectedCount) {
        super(maxEntries, expectedCount);
    }

    public void set(T key, double val) {
        distrib.put(key, val);
    }

    public void add(T key, double toAdd) {
        distrib.addTo(key, toAdd);
    }

    /**
     * Adds the given density distribution to this one. Does not scale the inputs; does not normalize the resulting
     * values
     * @param add
     */
    public void addMultinomial(Multinomial<?> add) {
        ObjectIterator<? extends Entry<?>> iter = add.distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<?> entry = iter.next();
            add((T) entry.getKey(), entry.getDoubleValue());
        }
        if (this.maxEntries > 0 && this.distrib.size() > this.maxEntries) {
            this.maxEntries = this.distrib.size();
        }
    }

    /**
     * Takes the input distribution and adds it to this distribution but WEIGHTS each of the entries by how certain
     * the incoming distribution is (i.e. is it maximum entropy aka
     * @param add
     */
    public void addEntropyScaled(Multinomial<T> add) {
        if (add.isEmpty()) {
            return;
        }
        try {
            Preconditions.checkState(this.maxEntries > 0, "cannot use when you dont know the max discrete state count");
            Preconditions.checkArgument(this.maxEntries == add.maxEntries,
                    "different maxes ",
                    this.maxEntries,
                    add.maxEntries
            );
            // the perc of entropy is a measure of how uncertain you are-- the more entropy the more uncertainty
            // so scaling factor is inverse of that
            double scaling = 1.0 - (add.entropy() / maxEntropy());
//        double scaling = add.entropy() / maxEntropy();
            ObjectIterator<Entry<T>> iter = add.distrib.object2DoubleEntrySet().fastIterator();
            while (iter.hasNext()) {
                Entry<T> entry = iter.next();
                add(entry.getKey(), entry.getDoubleValue() * scaling);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Problem adding entropy scaled for " + add.toString(), e);
        }
    }

    public void addAll(Iterable<T> items, double amount) {
        for (T item : items) {
            add(item, amount);
        }
    }

    public void scaleAllPresent(double scalar) {
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            set(entry.getKey(), entry.getDoubleValue() * scalar);
        }
    }

    /**
     * Takes this multinomial which is made up of n observations (i.e. the sum of all counts) and scales it
     * down to virtualCount observations (just a linear scaling which might not be a good idea)
     * @param virtualCount
     */
    public void scaleToVirtualCount(double virtualCount) {
        double sum = sum();
        if (sum <= 0) return; // nothing to scale
        double factor = virtualCount / sum;
        scaleAllPresent(factor);
    }

    public void addToAllPresent(double amount) {
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            add(entry.getKey(), amount);
        }
    }

    public MutableMultinomial<T> normalize() {
        if (this.maxEntries > 0) {
            Preconditions.checkState(distrib.size() <= this.maxEntries,
                    "table exceeded maxEntries",
                    distrib.keySet().size(),
                    this.maxEntries
            );
        }
        double sum = sum();
        if (sum <= 0) return this;
        for (Entry<T> next : distrib.object2DoubleEntrySet()) {
            distrib.put(next.getKey(), next.getDoubleValue() / sum);
        }
        return this;
    }

    public Multinomial<T> toImmutable() {
        // this is going to be immutable so if max is set as i dont know -- then update to current count
        int max = maxEntries;
        if (max <= 0) {
            max = this.distrib.size();
        }
        return new Multinomial<>(max, new Object2DoubleOpenHashMap<>(this.distrib));
    }

    public void remove(T toRemove) {
        this.distrib.removeDouble(toRemove);
    }
}
