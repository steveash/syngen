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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.AbstractIterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Sometimes you have a list of slots where each slot can have diff # of entries and you want
 * to iterate through all positions in the slot list.
 * [A=3 entries] [B=2 entries] [C=4 entries]
 * then the total permutations [A, B, C] would be like [0, 0, 0], [0, 0, 1], .. [0,0,3], [0, 1, 0], ..., [2,1,3]
 * @author Steve Ash
 */
public class SlotIndexIterator extends AbstractIterator<IntArrayList> {

    public static Iterator<IntArrayList> makeFor(List<? extends List<?>> slots) {
        if (slots.isEmpty()) {
            return Collections.emptyIterator();
        }
        IntArrayList sizes = new IntArrayList(slots.size());
        for (int i = 0; i < slots.size(); i++) {
            List<?> slot = slots.get(i);
            if (slot.isEmpty()) {
                return Collections.emptyIterator();
            }
            sizes.add(slot.size());
        }
        return new SlotIndexIterator(sizes);
    }

    private SlotIndexIterator(IntArrayList slotSizes) {
        // cant pass in any zeroes in the size array
        this.slotIndexes = new IntArrayList(slotSizes.size());
        for (int i = 0; i < slotSizes.size(); i++) {
            slotIndexes.add(0); // start at zero
        }
        this.slotSizes = slotSizes;
    }

    private final IntArrayList slotIndexes;
    private final IntArrayList slotSizes;
    private boolean firstCompute = true;

    @Override
    protected IntArrayList computeNext() {
        if (!firstCompute) {
            // find the next combo
            if (!updateSlotIndexes()) {
                return endOfData();
            }
        } else {
            firstCompute = false; // not first now
        }
        // if we're here we've set the slots to a valid combo
        return new IntArrayList(slotIndexes);
    }

    private boolean updateSlotIndexes() {
        for (int i = 0; i < slotIndexes.size(); i++) {
            int next = slotIndexes.getInt(i) + 1;
            if (next >= slotSizes.getInt(i)) {
                // we've exceeded this, so reset this counter to 0 and go to the next slot
                slotIndexes.set(i, 0);
            } else {
                // we successfully increment one entry so we're cool to return this one
                slotIndexes.set(i, next);
                return true;
            }
        }
        // we went through the all and found none to increment so we're done
        return false;
    }
}
