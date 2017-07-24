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

import org.apache.commons.lang3.StringUtils;

import com.github.steveash.synthrec.reducer.TaggedValue;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * converts strings into "shapes" of themselves where all A-Z letters are collapsed into a sequence of AAAA's
 * where the length is an approximate (slightly binned) length of the original.  Numbers are similarly
 * collapsed. Puncution is left as is and any non-printable characters are just truncated entirely.
 * @see PatternReducer
 * @see PatternExpander
 * @author Steve Ash
 */
public class StringBinner {

    public static final String PATTERN_PREFIX = "<!BINNER!>=";

    public static final boolean isTagged(String value) {
        return TaggedValue.isTaggedWith(PATTERN_PREFIX, value);
    }

    public static String unTag(String value) {
        Preconditions.checkArgument(isTagged(value), "didnt pass a pattern", value);
        return TaggedValue.unTag(PATTERN_PREFIX, value);
    }

    public static String tag(String pattern) {
        Preconditions.checkArgument(!isTagged(pattern), "already prefixed", pattern);
        return TaggedValue.tag(PATTERN_PREFIX, pattern);
    }

    private static final CharMatcher alpha = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));
    private static final CharMatcher numeric = CharMatcher.inRange('0', '9');
    private static final CharMatcher ws = CharMatcher.whitespace();
    private static final CharMatcher printable = CharMatcher.invisible()
            .negate()
            .and(CharMatcher.whitespace().negate());

    public static String reduceAndTag(String input) {
        return tag(reduce(input));
    }

    public static String reduce(String input) {
        // simple cases
        if (StringUtils.isBlank(input)) {
            return "";
        }
        if (alpha.matchesAllOf(input)) {
            return StringUtils.repeat('A', binLength(input));
        }
        if (numeric.matchesAllOf(input)) {
            return StringUtils.repeat('9', binLength(input));
        }
        // complex case
        StringBuilder overall = new StringBuilder();
        StringBuilder segment = new StringBuilder();
        char segmentType = '\0'; // no segment
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            char charType = calcCharType(c);
            if (charType == '\0') {
                continue;
            }
            if (charType != segmentType) {
                dumpSegment(overall, segment);
                segmentType = charType;
            }
            segment.append(c);
        }
        dumpSegment(overall, segment);
        return overall.toString();
    }

    private static void dumpSegment(StringBuilder overall, StringBuilder segment) {
        if (segment.length() > 0) {
            String wholeSegment = segment.toString();
            if (alpha.matchesAllOf(wholeSegment)) {
                wholeSegment = StringUtils.repeat('A', binLength(wholeSegment));
            } else if (numeric.matchesAllOf(wholeSegment)) {
                wholeSegment = StringUtils.repeat('9', binLength(wholeSegment));
            } else if (ws.matchesAllOf(wholeSegment)) {
                wholeSegment = " ";
            }
            overall.append(wholeSegment);
            segment.delete(0, segment.length()); // clear it for next time
        }
    }

    private static char calcCharType(char c) {
        if (alpha.matches(c)) {
            return 'A';
        }
        if (numeric.matches(c)) {
            return '9';
        }
        if (ws.matches(c)) {
            return ' ';
        }
        if (printable.matches(c)) {
            return c; // some punctuation that we want to preserve as is
        }
        return '\0'; // we just trim non-printable chars
    }

    public static int binLength(String s) {
        int c = s.length();
        return binLength(c);
    }

    private static int binLength(int c) {
        if (c < 8) return c;
        if (c < 10) return 10;
        if (c < 13) return 13;
        if (c < 16) return 16;
        if (c < 20) return 20;
        if (c < 24) return 24;
        if (c < 28) return 28;
        if (c < 35) return 35;
        if (c < 42) return 42;
        if (c < 60) return 60;
        if (c < 78) return 78;
        if (c < 100) return 100;
        return 200;
    }
}
