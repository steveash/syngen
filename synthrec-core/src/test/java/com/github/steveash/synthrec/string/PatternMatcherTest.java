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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Steve Ash
 */
public class PatternMatcherTest {

    @Test
    public void shouldTestSimple() throws Exception {
        PatternMatcher matcher = PatternMatcher.builder()
                .matchExact("abc")
                .matchExact("def")
                .matchRegex("[a-z]+[0-9]+")
                .build();
        assertTrue(matcher.matches("abc"));
        assertTrue(matcher.matches("def"));
        assertTrue(matcher.matches("aaabcdefdddf1232123"));

        assertFalse(matcher.matches("aaabcdefdddf1232asdf123"));
        assertFalse(matcher.matches("ABC"));
        assertFalse(matcher.matches("xyz"));
    }

    @Test
    public void shouldTestEmpty() throws Exception {
        PatternMatcher matcher = PatternMatcher.builder().build();
        assertFalse(matcher.matches("abc"));
    }

    @Test
    public void shouldTestMissingregex() throws Exception {
        PatternMatcher matcher = PatternMatcher.builder()
                .matchExact("abc")
                .matchExact("def")
                .build();
        assertTrue(matcher.matches("abc"));
        assertTrue(matcher.matches("def"));

        assertFalse(matcher.matches("aaabcdefdddf1232123"));
        assertFalse(matcher.matches("aaabcdefdddf1232asdf123"));
        assertFalse(matcher.matches("ABC"));
        assertFalse(matcher.matches("xyz"));
    }

    @Test
    public void shouldTestMissingExact() throws Exception {
        PatternMatcher matcher = PatternMatcher.builder()
                .matchRegex("[a-z]+[0-9]+")
                .build();
        assertTrue(matcher.matches("aaabcdefdddf1232123"));
        assertTrue(matcher.matches("a0"));

        assertFalse(matcher.matches("abc"));
        assertFalse(matcher.matches("def"));
        assertFalse(matcher.matches("aaabcdefdddf1232asdf123"));
        assertFalse(matcher.matches("ABC"));
        assertFalse(matcher.matches("xyz"));
    }
}