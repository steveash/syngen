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

package com.github.steveash.synthrec.count;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ash
 */
public class DistribVocabRegistryTest {

    private DistribVocabRegistry reg;

    @Before
    public void setUp() throws Exception {
        reg = new DistribVocabRegistry();
    }

    @Test
    public void shouldResolveNormalDistribs() throws Exception {
        short nameCode = reg.resolveDistribCode("name");
        assertEquals(1, reg.resolveValueIndexFor("name", "steve"));
        assertEquals(2, reg.resolveValueIndexFor("name", "bob"));
        assertEquals(1, reg.resolveValueIndexFor("gender", "male"));
        short genderCode = reg.resolveDistribCode("gender");
        assertEquals("steve", reg.resolveValueForIndex(nameCode, 1));
        assertEquals("bob", reg.resolveValueForIndex(nameCode, 2));
        assertEquals("male", reg.resolveValueForIndex(genderCode, 1));

        try {
            reg.resolveValueForIndex(nameCode, 3);
            fail();
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void shouldResolveHierarchicalDistrib() throws Exception {
        short nameCode = reg.resolveDistribSubFieldCode("name", "first");
        assertEquals(1, reg.resolveSubFieldValueIndexFor("name", "first", "stevo"));
        assertEquals(2, reg.resolveSubFieldValueIndexFor("name", "first", "bobo"));
        assertEquals(1, reg.resolveSubFieldValueIndexFor("name", "last", "ash"));
        short nameLastCode = reg.resolveDistribSubFieldCode("name", "last");
        assertEquals("ash", reg.resolveSubFieldValueForIndex(nameLastCode, 1));
        assertEquals("stevo", reg.resolveSubFieldValueForIndex(nameCode, 1));
        assertEquals("bobo", reg.resolveSubFieldValueForIndex(nameCode, 2));
        try {
            assertEquals("bobo", reg.resolveSubFieldValueForIndex(nameCode, 3));
            fail();
        } catch (Exception e) {
            //
        }
    }
}