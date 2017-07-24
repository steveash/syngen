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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;

/**
 * String normalizer that is geared towards names; this includes the general normalizer
 */
public class NameNormalizer implements StringNormalizer {

    public static final NameNormalizer INSTANCE = new NameNormalizer();

    private static final CharMatcher spaceMatches = CharMatcher.anyOf(",^|-()[]%*_{}\\\\;:><\\?~\"").precomputed();
    private static final CharMatcher collapseMatches = CharMatcher.anyOf("/.'`!&+").precomputed();
    private static final Pattern repeatingPrefix = Pattern.compile("(\\w)\\1{2,}(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern squashedInitial = Pattern.compile("\\s*([a-z])\\.([a-z][a-z]+(?:\\s.*)?)", Pattern.CASE_INSENSITIVE);
    private static final CharMatcher whiteSpace = CharMatcher.whitespace();

    @Override
    public String normalize(String input) {
        input = GeneralNormalizer.INSTANCE.normalize(input);
        Matcher siMatcher = squashedInitial.matcher(input);
        if (siMatcher.matches()) {
            input = siMatcher.group(1) + " " + siMatcher.group(2);
        }

        input = spaceMatches.replaceFrom(input, ' ');
        input = collapseMatches.removeFrom(input);
        input = whiteSpace.trimAndCollapseFrom(input, ' ');

        Matcher matcher = repeatingPrefix.matcher(input);
        if (matcher.matches() && !NameSuffix.isAnyKnownSuffix(input)) {
            input = matcher.group(2);
        }
        return input;
    }
}
