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

import static com.google.common.base.CharMatcher.WHITESPACE;
import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.CharMatcher.whitespace;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Utilities for letter related information
 * @author Steve Ash
 */
public class Graphemes {

    private static final CharMatcher vowels = anyOf("AEIOUYaeiouy").precomputed();
    private static final CharMatcher consonants = (inRange('A', 'Z').or(inRange('a', 'z'))).and(vowels.negate())
            .precomputed();
    private static final CharMatcher other = CharMatcher.any().and(vowels.or(consonants).negate()).precomputed();
    private static final CharMatcher digits = CharMatcher.digit().precomputed();

    public static boolean isVowelOrConsonant(String graph) {
        Preconditions.checkArgument(graph.length() == 1);
        return !other.matches(graph.toUpperCase().charAt(0));
    }

    public static boolean isVowel(String graph) {
        Preconditions.checkArgument(graph.length() == 1);
        return vowels.matches(graph.toUpperCase().charAt(0));
    }

    public static int vowelCount(String word) {
        return vowels.countIn(word);
    }

    public static double vowelPerc(String normalWord) {
        return ((double) Graphemes.vowelCount(normalWord)) / (double) normalWord.length();
    }

    public static boolean isConsonant(String graph) {
        Preconditions.checkArgument(graph.length() == 1);
        return consonants.matches(graph.toUpperCase().charAt(0));
    }

    public static boolean isAllVowelsOrConsonants(String word) {
        return !other.matchesAnyOf(word);
    }

    public static boolean isAllVowels(String word) {
        return vowels.matchesAllOf(word);
    }

    public static boolean isAnyVowels(String word) {
        return vowels.matchesAnyOf(word);
    }

    public static boolean isAllConsonants(String word) {
        return consonants.matchesAllOf(word);
    }

    public static boolean isAnyConsonants(String word) {
        return consonants.matchesAnyOf(word);
    }

    public static boolean isAnyDigits(String word) {
        return digits.matchesAnyOf(word);
    }

    public static boolean isCountDigits(String word, int minGoodCount, int maxGoodCount) {
        int count = digits.countIn(word);
        return count >= minGoodCount && count <= maxGoodCount;
    }

    public static boolean isAllDigits(String word) {
        return digits.matchesAllOf(word);
    }

    public static String lastChar(String word) {
        return String.valueOf(word.charAt(word.length() - 1));
    }

    /**
     * Converts shape replacing every consonent with a 'c', vowel with a 'v' and leaves all punctuation
     * @param input
     * @return
     */
    public static String reduceLetters(String input) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Graphemes.consonants.matches(c)) {
                sb.append('c');
            } else if (Graphemes.vowels.matches(c)) {
                sb.append('v');
            } else if (whitespace().matches(c)) {
                sb.append('s');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
