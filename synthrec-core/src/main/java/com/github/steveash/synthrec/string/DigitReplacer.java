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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.stat.RandUtil;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Helper class for strings that contain digits or patterns of digits (see Pattern Feature in the gen project)
 * @author Steve Ash
 */
public class DigitReplacer {

    public static final CharMatcher DIGITS = CharMatcher.inRange('0', '9');
    public static final CharMatcher NON_ZERO_DIGITS = CharMatcher.inRange('1', '9');

    /**
     * Replaces the digits in a given pattern with random digits, optionally restricting the first
     * digit to be non zero
     * @param rand
     * @param pattern
     * @param noLeadingZero
     * @return
     */
    public static String replaceDigits(RandomGenerator rand, String pattern, boolean noLeadingZero) {
        int toReplace = NON_ZERO_DIGITS.countIn(pattern);
        String source = randomDigits(rand, toReplace, noLeadingZero);
        return replacePatternLtoR(rand, pattern, source);
    }

    /**
     * Replaces the '9' pattern digit from the pattern string with the source string from left to right
     * @param pattern
     * @param source
     * @return
     */
    public static String replacePatternLtoR(RandomGenerator rand, String pattern, String source) {
        int sidx = firstDigitLtoR(source, 0);
        StringBuilder sb = new StringBuilder(pattern.length());
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (!DIGITS.matches(c)) {
                sb.append(c);
                continue;
            }
            if (c == '0') {
                sb.append(c);
                sidx = firstDigitLtoR(source, sidx + 1);
                continue;
            }
            if (sidx < 0) {
                sb.append(1 + rand.nextInt(9));
            } else {
                sb.append(source.charAt(sidx));
                sidx = firstDigitLtoR(source, sidx + 1);
            }
        }
        String result = sb.toString();
        Preconditions.checkState(result.length() == pattern.length());
        return result;
    }

    /**
     * Replaces the '9' pattern digit from the pattern string with the source string from right to left
     * @param pattern
     * @param source
     * @return
     */
    public static String replacePatternRtoL(RandomGenerator rand, String pattern, String source) {
        int sidx = firstDigitRtoL(source, source.length() - 1);
        StringBuilder sb = new StringBuilder(pattern.length());
        for (int i = pattern.length() - 1; i >= 0; i--) {
            char c = pattern.charAt(i);
            if (!DIGITS.matches(c)) {
                sb.append(c);
                continue;
            }
            if (c == '0') {
                sb.append(c);
                sidx = firstDigitRtoL(source, sidx - 1);
                continue;
            }
            if (sidx < 0) {
                sb.append(1 + rand.nextInt(9));
            } else {
                sb.append(source.charAt(sidx));
                sidx = firstDigitRtoL(source, sidx - 1);
            }
        }
        String result = sb.reverse().toString();
        Preconditions.checkState(result.length() == pattern.length());
        return result;
    }

    /**
     * Returns the first 0-9 digit from the value string starting at the given location
     * @param value
     * @param startingAt the place to start; if < 0 then this wont even bother looking, it will just return -1
     * @return
     */
    public static int firstDigitLtoR(String value, int startingAt) {
        if (startingAt == -1 || startingAt >= value.length()) {
            return -1;
        }
        for (int i = startingAt; i < value.length(); i++) {
            if (DIGITS.matches(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the first 0-9 digit from the value string starting at the given location
     * @param value
     * @param startingAt the place to start; if < 0 then this wont even bother looking, it will just return -1
     * @return
     */
    public static int firstDigitRtoL(String value, int startingAt) {
        if (startingAt == -1 || startingAt >= value.length()) {
            return -1;
        }
        for (int i = startingAt; i >= 0; i--) {
            if (DIGITS.matches(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public static String randomDigits(RandomGenerator rand, int length, boolean noLeadingZero) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            if (i == 0 && noLeadingZero) {
                sb.append(1 + rand.nextInt(9));
            } else {
                sb.append(rand.nextInt(10));
            }
        }
        return sb.toString();
    }
}
