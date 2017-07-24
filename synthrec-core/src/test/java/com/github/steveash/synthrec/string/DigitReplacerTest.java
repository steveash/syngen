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

import static com.github.steveash.synthrec.string.DigitReplacer.firstDigitLtoR;
import static com.github.steveash.synthrec.string.DigitReplacer.firstDigitRtoL;
import static com.github.steveash.synthrec.string.DigitReplacer.replacePatternLtoR;
import static com.github.steveash.synthrec.string.DigitReplacer.replacePatternRtoL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Steve Ash
 */
public class DigitReplacerTest {

    private RandomGenerator rand;

    @Before
    public void setUp() throws Exception {
        rand = Mockito.mock(RandomGenerator.class);
        AtomicInteger seq = new AtomicInteger(0);
        when(rand.nextInt(anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                return seq.getAndIncrement();
            }
        });
    }

    @Test
    public void shouldLeftToRight() throws Exception {
        assertEquals(-1, firstDigitLtoR("123,45", -1));
        assertEquals(-1, firstDigitLtoR("123,45", 1238));
        assertEquals(0, firstDigitLtoR("123,45", 0));
        assertEquals(1, firstDigitLtoR("123,45", 1));
        assertEquals(4, firstDigitLtoR("123,45", 3));
        assertEquals(5, firstDigitLtoR("123,45 0", 5));
        assertEquals(7, firstDigitLtoR("123,45 0", 6));

        assertEquals("123-45-67", replacePatternLtoR(rand, "999-99-99", "123-45-6789"));
        assertEquals("123-45-678", replacePatternLtoR(rand, "999-99-999", "123-45-6789"));
        assertEquals("123-45-6789", replacePatternLtoR(rand, "999-99-9999", "123-45-6789"));
        assertEquals("123-45-67891", replacePatternLtoR(rand, "999-99-99999", "123-45-6789"));
        assertEquals("123-45-678923", replacePatternLtoR(rand, "999-99-999999", "123-45-6789"));
        assertEquals("123-45-6789456", replacePatternLtoR(rand, "999-99-9999999", "123-45-6789"));

        assertEquals("123-45-078", replacePatternLtoR(rand, "999-99-099", "123-45-6789"));
        assertEquals("123-00-6789", replacePatternLtoR(rand, "999-00-9999", "123-45-6789"));
    }

    @Test
    public void shouldRightToLeft() throws Exception {
        assertEquals(-1, firstDigitRtoL("123,45", -1));
        assertEquals(-1, firstDigitRtoL("123,45", 1238));
        assertEquals(0, firstDigitRtoL("123,45", 0));
        assertEquals(1, firstDigitRtoL("123,45", 1));
        assertEquals(2, firstDigitRtoL("123,45", 3));
        assertEquals(5, firstDigitRtoL("123,45 0", 5));
        assertEquals(5, firstDigitRtoL("123,45 0", 6));

        assertEquals("345-67-89", replacePatternRtoL(rand, "999-99-99", "123-45-6789"));
        assertEquals("234-56-789", replacePatternRtoL(rand, "999-99-999", "123-45-6789"));
        assertEquals("123-45-6789", replacePatternRtoL(rand, "999-99-9999", "123-45-6789"));
        assertEquals("112-34-56789", replacePatternRtoL(rand, "999-99-99999", "123-45-6789"));
        assertEquals("321-23-456789", replacePatternRtoL(rand, "999-99-999999", "123-45-6789"));
        assertEquals("654-12-3456789", replacePatternRtoL(rand, "999-99-9999999", "123-45-6789"));

        assertEquals("123-45-6089", replacePatternRtoL(rand, "999-99-9099", "123-45-6789"));
        assertEquals("103-45-6089", replacePatternRtoL(rand, "909-99-9099", "123-45-6789"));
    }
}