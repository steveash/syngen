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

package com.github.steveash.synthrec.string;

import static com.google.common.base.CharMatcher.ascii;
import static com.google.common.base.CharMatcher.javaIsoControl;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.github.steveash.synthrec.reducer.TaggedValue;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Similar to the string binner but doesn't bin the consecutive A's or 9's
 * @author Steve Ash
 */
public class PatternReducer {

    public static final CharMatcher LETTERS_DIGITS_AND_PUNC = ascii().and(javaIsoControl().negate()).precomputed();
    public static final CharMatcher SPACES = CharMatcher.whitespace();
    public static final Pattern COLLAPSE_WS_AROUND_PUNC = Pattern.compile("(?:([0-9])\\s*([~^()\\-_+=';:,.<>]+)\\s*([0-9])|([a-z])\\s*([@`\\-_]+)\\s*([a-z]))", Pattern.CASE_INSENSITIVE);
    public static final CharMatcher LETTERS = CharMatcher.javaLetter();
    public static final CharMatcher PUNC = LETTERS_DIGITS_AND_PUNC.and(LETTERS.or(DigitReplacer.DIGITS).negate());

    public static final String PATTERN_PREFIX = "<!PATTERN!>=";
    public static final String START_ESC = "{{";
    public static final String END_ESC = "}}";

    public static final boolean isTagged(String value) {
        return TaggedValue.isTaggedWith(PATTERN_PREFIX, value);
    }

    public static String unTag(String value) {
        Preconditions.checkArgument(isTagged(value),"didnt pass a pattern", value);
        return TaggedValue.unTag(PATTERN_PREFIX, value);
    }

    public static String tag(String pattern) {
        Preconditions.checkArgument(!isTagged(pattern),"already prefixed", pattern);
        return PATTERN_PREFIX + pattern;
    }

    public static String replaceAndTag(String input) {
        return tag(replace(input));
    }

    /**
     * Takes a string and replaces it with a "pattern" that preserves the punctuation and leading/trailing
     * zeroes but otherwise reduces letters to A and digits to 9 -- also trims/collpases whitespace
     * @param field
     * @return
     */
    public static String replace(String field) {
        if (containsEscaped(field)) {
            field = replaceWithEscapes(field);
        } else {
            field = replaceInToken(field);
        }
        field = onlyRetainLeadingSuffixZeroes(field);
        return field;
    }

    private static String replaceWithEscapes(String field) {
        // split field into tokens preserving the paired bracket escapes
        StringBuilder sb = new StringBuilder(field.length());
        int pending = 0;
        int start = 0;
        int i = 0;
        while (i < field.length()) {
            if (matchesAt(field, START_ESC, i)) {
                if (pending == 0) {
                    // crossing a boundary
                    sb.append(replaceInToken(field.substring(start, i)));
                    start = i;
                }
                pending += 1;
                i += START_ESC.length() - 1; // skip the rest of the starting sequence that we matches
            } else if (matchesAt(field, END_ESC, i) && pending > 0) {
                pending -= 1;
                if (pending == 0) {
                    // dont replace because this is an escaped string
                    sb.append(field.substring(start, i + END_ESC.length()));
                    start = i + END_ESC.length(); // bump to next
                }
                i += END_ESC.length() - 1; // skip the next char of the end token
            }
            i += 1;
        }
        if (i - start > 0) {
            sb.append(replaceInToken(field.substring(start, i)));
        }
        return sb.toString();
    }

    static boolean matchesAt(String input, String match, int startingAt) {
        for (int i = 0; i < match.length(); i++) {
            if (startingAt + i >= input.length()) {
                return false;
            }
            char a = input.charAt(startingAt + i);
            char b = match.charAt(i);
            if (a != b) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsEscaped(String field) {
        return field.contains(START_ESC);
    }

    private static String replaceInToken(String field) {
        if (field.length() == 0) {
            return field;
        }
        field = field.toUpperCase();
        field = SPACES.trimAndCollapseFrom(field, ' ');
        field = COLLAPSE_WS_AROUND_PUNC.matcher(field).replaceAll("$1$2$3");
        field = DigitReplacer.NON_ZERO_DIGITS.replaceFrom(field, '9');
        field = LETTERS.replaceFrom(field, 'A');
        return field;
    }

    // if there is more than one leading (or triling) zero then we're going to preserve it (i.e. 6 leading zeroes is interesting)
    private static String onlyRetainLeadingSuffixZeroes(String field) {
        int leadingZeroCount = 0;
        int start = 0;
        for (int i = start; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == '0') {
                start += 1;
                leadingZeroCount += 1;
            } else if (PUNC.matches(c)) {
                start += 1; // we skip punc
            } else {
                break;
            }
        }
        if (leadingZeroCount == field.length()) {
            return field;
        }
        int trailingZeroCount = 0;
        int end = field.length() - 1;
        for (int i = end; i >= 0; i--) {
            char c = field.charAt(i);
            if (c == '0') {
                end -= 1;
                trailingZeroCount += 1;
            } else if (PUNC.matches(c)) {
                end -= 1;
            } else {
                break;
            }
        }
        if (leadingZeroCount < 2) {
            start = 0;
        }
        if (trailingZeroCount < 2) {
            end = field.length() - 1;
        }
        StringBuilder sb = null;
        for (int i = start; i <= end; i++) {
            if (field.charAt(i) == '0') {
                if (sb == null) {
                    sb = new StringBuilder(field);
                }
                // we only care about leading and trailing zeroes
                sb.setCharAt(i, '9');
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return field; // nothing to change
    }
}
