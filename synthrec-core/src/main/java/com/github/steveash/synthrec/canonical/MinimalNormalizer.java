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

package com.github.steveash.synthrec.canonical;

import com.google.common.base.CharMatcher;

/**
 * @author Steve Ash
 */
public class MinimalNormalizer implements StringNormalizer {

    public static final MinimalNormalizer INSTANCE = new MinimalNormalizer();

    private final CharMatcher replaceWithSpace = CharMatcher.anyOf("|").or(CharMatcher.invisible()).precomputed();

    @Override
    public String normalize(String input) {
        return replaceWithSpace.replaceFrom(input, ' ')
                .toUpperCase()
                .trim();
    }
}
