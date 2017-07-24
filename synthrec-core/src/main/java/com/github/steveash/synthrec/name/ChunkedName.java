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

package com.github.steveash.synthrec.name;

/**
 * @author Steve Ash
 */
public class ChunkedName {

    private final int splitIndex;
    private final NameSegment segmentLtSplit;
    private final NameSegment segmentGteSplit;

    public ChunkedName(int splitIndex, NameSegment segmentLtSplit, NameSegment segmentGteSplit) {
        this.splitIndex = splitIndex;
        this.segmentLtSplit = segmentLtSplit;
        this.segmentGteSplit = segmentGteSplit;
    }

    /**
     * @return index of the first token that switches segments OR -1 if all tokens are the same segment chunk
     */
    public int splitIndex() {
        return splitIndex;
    }

    public boolean hasSplitIndex() {
        return splitIndex >= 0;
    }

    /**
     * @return the segment of all of the tokens from [0, splitIndex) (or if split index -1 then this will just
     * return the segment type of the whole token sequence)
     */
    public NameSegment segmentBeforeSplit() {
        return segmentLtSplit;
    }

    public NameSegment segmentAfterAndEqualToSplit() {
        return segmentGteSplit;
    }

    public NameSegment segmentAt(int index) {
        if (hasSplitIndex()) {
            if (index < splitIndex) {
                return segmentLtSplit;
            } else {
                return segmentGteSplit;
            }
        } else {
            return segmentLtSplit;
        }
    }
}
