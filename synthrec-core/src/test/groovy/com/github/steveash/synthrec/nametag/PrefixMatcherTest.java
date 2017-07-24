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

package com.github.steveash.synthrec.nametag;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
public class PrefixMatcherTest {

    @Test
    public void shouldMatch() throws Exception {
        PrefixMatcher matcher = new PrefixMatcher(Lists.newArrayList("A", "BB", "CCC", "DDDD"));
        assertFalse(matcher.matches("Z"));
        assertFalse(matcher.matches("ZA"));
        assertFalse(matcher.matches(""));
        assertFalse(matcher.matches("B"));
        assertFalse(matcher.matches("CC"));
        assertTrue(matcher.matches("A"));
        assertTrue(matcher.matches("AB"));
        assertTrue(matcher.matches("ABC"));
        assertTrue(matcher.matches("BB"));
        assertTrue(matcher.matches("BBC"));
        assertTrue(matcher.matches("BBCD"));
        assertTrue(matcher.matches("CCC"));
        assertTrue(matcher.matches("CCCCCC"));
        assertTrue(matcher.matches("CCC123"));
    }
}