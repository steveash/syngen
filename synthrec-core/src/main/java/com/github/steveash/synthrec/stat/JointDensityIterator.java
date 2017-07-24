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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.carrotsearch.hppc.IntArrayList;
import com.github.steveash.synthrec.stat.JointDensityIterator.JointEntry;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Takes multiple densities and iterates through all possible assignments with some > 0 probabiliy mass
 * @author Steve Ash
 */
public class JointDensityIterator extends AbstractIterator<JointEntry> {

    public static class JointEntry {
        private final Object[] entries;
        private final double[] probs;

        public JointEntry(Object[] entries, double[] probs) {
            this.entries = entries;
            this.probs = probs;
        }

        public Object entry(int i) {
            return entries[i];
        }

        public double probability(int i) {
            return probs[i];
        }

        public Object[] entries() {
            return entries;
        }
    }

    public JointDensityIterator(Multinomial<?>... densities) {
        this(Arrays.asList(densities));
    }

    public JointDensityIterator(List<? extends Multinomial<?>> densities) {
        slotEntry = Lists.newArrayListWithCapacity(densities.size());
        slotEntryProb = Lists.newArrayListWithCapacity(densities.size());
        slotEntryIndex = new IntArrayList(densities.size());
        // go ahead and get out the good probabilities and put them into the slots for easy iteration later
        for (Multinomial<?> density : densities) {
            ArrayList<Object> entries = Lists.newArrayList();
            DoubleArrayList entryProbs = new DoubleArrayList();

            ObjectIterator<? extends Entry<?>> iter = density.entries().fastIterator();
            while (iter.hasNext()) {
                Entry<?> entry = iter.next();
                if (entry.getDoubleValue() > 0) {
                    entries.add(entry.getKey());
                    entryProbs.add(entry.getDoubleValue());
                }
            }
            Preconditions.checkState(entries.size() > 0, "cannot joint walk a density with no mass", densities);
            slotEntry.add(entries);
            slotEntryProb.add(entryProbs);
            slotEntryIndex.add(0); // start at first combo
        }
        this.slotSize = densities.size();
    }

    private final List<List<Object>> slotEntry;
    private final List<DoubleArrayList> slotEntryProb;
    private final IntArrayList slotEntryIndex;
    private final int slotSize;
    private boolean firstCompute = true;

    @Override
    protected JointEntry computeNext() {
        if (!firstCompute) {
            // find the next combo
            if (!updateSlotIndexes()) {
                return endOfData();
            }
        } else {
            firstCompute = false; // not first now
        }
        // if we're here we've set the slots to a valid combo
        return makeEntry();
    }

    private JointEntry makeEntry() {
        Object[] entries = new Object[slotSize];
        double[] probs = new double[slotSize];
        for (int i = 0; i < slotSize; i++) {
            int slotIndex = slotEntryIndex.get(i);
            entries[i] = slotEntry.get(i).get(slotIndex);
            probs[i] = slotEntryProb.get(i).getDouble(slotIndex);
        }
        return new JointEntry(entries, probs);
    }

    private boolean updateSlotIndexes() {
        for (int i = 0; i < slotSize; i++) {
            int next = slotEntryIndex.get(i) + 1;
            if (next >= slotEntry.get(i).size()) {
                // we've exceeded this, so reset this counter to 0 and go to the next slot
                slotEntryIndex.set(i, 0);
            } else {
                // we successfully increment one entry so we're cool to return this one
                slotEntryIndex.set(i, next);
                return true;
            }
        }
        // we went through the all and found none to increment so we're done
        return false;
    }
}
