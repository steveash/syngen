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

package com.github.steveash.synthrec.collect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ash
 */
public class VocabularyTest {

    private Vocabulary<String> vocab;

    @Before
    public void setUp() throws Exception {
        vocab = new Vocabulary<>();
    }

    @Test
    public void shouldSimpleWork() throws Exception {
        assertEquals(1, vocab.putIfAbsent("steve"));
        assertEquals(2, vocab.putIfAbsent("bob"));

        assertTrue(vocab.contains("steve"));
        assertTrue(vocab.contains("bob"));
        assertFalse(vocab.contains("zane"));

        assertEquals("steve", vocab.getForIndex(1));
        assertEquals("bob", vocab.getForIndex(2));
        try {
            assertEquals(null, vocab.getForIndex(0));
            fail();
        } catch (Exception e) {
            //
        }
        try {
            assertEquals(null, vocab.getForIndex(3));
            fail();
        } catch (Exception e) {
            //
        }
        assertEquals(1, vocab.getIndexFor("steve"));
        assertEquals(2, vocab.getIndexFor("bob"));
        try {
            vocab.getIndexFor("ozooz");
            fail();
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void shouldUpdate() throws Exception {
        int index1 = vocab.putIfAbsent("steve");
        int index2 = vocab.putIfAbsent("bob");
        assertEquals("steve", vocab.getForIndex(index1));
        assertEquals(index1, vocab.updateIndexValue(index1, "ash"));

        assertEquals("ash", vocab.getForIndex(index1));
        assertEquals("bob", vocab.getForIndex(index2));
    }

    @Test
    public void shouldUpdate2() throws Exception {
        int index1 = vocab.putIfAbsent("steve");
        int index2 = vocab.putIfAbsent("bob");
        int index3 = vocab.putIfAbsent("mary");
        assertEquals("steve", vocab.getForIndex(index1));
        assertEquals(index2, vocab.updateIndexValue(index1, "bob"));

        assertEquals("bob", vocab.getForIndex(index1));
        assertEquals("bob", vocab.getForIndex(index2));

        assertEquals(index3, vocab.updateIndexValue(index2, "mary"));
        assertEquals("mary", vocab.getForIndex(index1));
        assertEquals("mary", vocab.getForIndex(index2));
        assertEquals("mary", vocab.getForIndex(index3));
    }
}