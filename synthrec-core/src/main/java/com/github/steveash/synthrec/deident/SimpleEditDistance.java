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

package com.github.steveash.synthrec.deident;

import java.util.Set;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.string.OptimalStringAlignment;
import com.github.steveash.synthrec.string.PatternReducer;
import com.github.steveash.synthrec.string.StringBinner;
import com.google.common.collect.ImmutableSet;

/**
 * A simple deident distance that (a) only uses edit distance as the comparison, (b) puts everything in one block
 * and therefore this is only useful for very simple, small distributions of strings
 * @author Steve Ash
 */
public class SimpleEditDistance implements DeidentDistance<String, String> {

    public static final SimpleEditDistance INSTANCE = new SimpleEditDistance();

    private static final Set<String> ONE_BLOCK = ImmutableSet.of("BLOCK");

    @Override
    public String makeVector(String input) {
        return input;   // no op
    }

    @Override
    public boolean isPublicDomain(String input) {
        if (Constants.MISSING.equals(input) ||
                StringBinner.isTagged(input) ||
                PatternReducer.isTagged(input))
        {
            return true;
        }
        return false;
    }

    @Override
    public double distance(String comp1, String comp2) {
        return OptimalStringAlignment.editDistanceNormalzied(comp1, comp2);
    }

    @Override
    public Set<String> blockingKeys(String input) {
        return ONE_BLOCK;
    }
}
