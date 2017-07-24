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

package com.github.steveash.synthrec.generator.reducer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
public class SimpleTokenReducerTest {

    private final ImmutableSet<String> publics = ImmutableSet.of("steve", "bob", "jon");
    private SimpleTokenReducer redu;

    @Before
    public void setUp() throws Exception {
        redu = new SimpleTokenReducer(100, publics::contains);
    }

    @Test
    public void shouldIdMatch() throws Exception {
        assertEquals("b", redu.reduceIfNecessary("b", 1000));
        assertEquals("bb", redu.reduceIfNecessary("bb", 1000));
        assertEquals("bbb", redu.reduceIfNecessary("bbb", 1000));

        assertEquals("<!PATTERN!>=A9", redu.reduceIfNecessary("b4", 1000));
        assertEquals("<!PATTERN!>=9A", redu.reduceIfNecessary("4b", 1000));
        assertEquals("<!PATTERN!>=9AA", redu.reduceIfNecessary("4bc", 1000));
        assertEquals("<!PATTERN!>=A9A", redu.reduceIfNecessary("b6c", 1000));
        assertEquals("<!PATTERN!>=A9A9", redu.reduceIfNecessary("b4c5", 1000));
        assertEquals("<!PATTERN!>=~A9A9", redu.reduceIfNecessary("~b4c5", 1000));
        assertEquals("<!PATTERN!>=AA9A9", redu.reduceIfNecessary("bd4c5", 1000));
        assertEquals("<!PATTERN!>=~AA9A9", redu.reduceIfNecessary("~bd4c5", 1000));
        assertEquals("<!PATTERN!>=AA9AA9", redu.reduceIfNecessary("bd4ce5", 1000));
        assertEquals("<!PATTERN!>=~AA9AA9", redu.reduceIfNecessary("~bd4ce5", 1000));
    }

    @Test
    public void shouldIdentReduceSimple() throws Exception {
        assertEquals("YEPPERS", redu.reduceIfNecessary("YEPPERS", 1000));
        assertEquals("YEPPERS21", redu.reduceIfNecessary("YEPPERS21", 1000));
        assertEquals("<!PATTERN!>={{steve}}99", redu.reduceIfNecessary("steve21", 1000));
        assertEquals("<!PATTERN!>=99{{bob}}", redu.reduceIfNecessary("21bob", 1000));
        assertEquals("<!PATTERN!>=99{{steve}}99", redu.reduceIfNecessary("21steve21", 1000));
        assertEquals("<!PATTERN!>=!!99{{steve}}99", redu.reduceIfNecessary("!!21steve21", 1000));
        assertEquals("<!PATTERN!>={{bob}}99{{steve}}", redu.reduceIfNecessary("bob21steve", 1000));
        assertEquals("<!PATTERN!>=!!{{bob}}99{{steve}}%$", redu.reduceIfNecessary("!!bob21steve%$", 1000));
        assertEquals("<!PATTERN!>=A99A", redu.reduceIfNecessary("m21s", 1000));
        assertEquals("<!PATTERN!>=A99{{steve}}", redu.reduceIfNecessary("b21steve", 1000));

        // these didn't match and are still too populated for reduction
        assertEquals("bc21steve", redu.reduceIfNecessary("bc21steve", 1000));
        assertEquals("steve21bc", redu.reduceIfNecessary("steve21bc", 1000));
        assertEquals("sammy21steve", redu.reduceIfNecessary("sammy21steve", 1000));
        assertEquals("sammy21s", redu.reduceIfNecessary("sammy21s", 1000));
    }

    @Test
    public void shouldReduceMoreRare() throws Exception {
        assertEquals("bc21steve", redu.reduceIfNecessary("bc21steve", 10));
        assertEquals("steve21bc", redu.reduceIfNecessary("steve21bc", 10));
        assertEquals("sammy21steve", redu.reduceIfNecessary("sammy21steve", 10));
        assertEquals("sammy21s", redu.reduceIfNecessary("sammy21s", 10));

        assertEquals("<!PATTERN!>=A", redu.reduceIfNecessary("b", 10));
        assertEquals("<!PATTERN!>=AA", redu.reduceIfNecessary("bc", 10));
        assertEquals("<!PATTERN!>=AAA", redu.reduceIfNecessary("bcd", 10));
        assertEquals("bob", redu.reduceIfNecessary("bob", 10));
        assertEquals("steve", redu.reduceIfNecessary("steve", 10));
    }

    @Test
    public void shouldNoOp() throws Exception {
        assertEquals("<!PATTERN!>=blahblah", redu.reduceIfNecessary("<!PATTERN!>=blahblah", 1000));
        assertEquals(" ", redu.reduceIfNecessary(" ", 1000));
    }
}