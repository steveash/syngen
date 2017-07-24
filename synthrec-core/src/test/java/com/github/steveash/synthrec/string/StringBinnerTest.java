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

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.steveash.synthrec.string.StringBinner;

/**
 * @author Steve Ash
 */
public class StringBinnerTest {

    @Test
    public void shouldBinSimple() throws Exception {
        assertEquals("AAAAA", StringBinner.reduce("STEVE"));
        assertEquals("AAAAA", StringBinner.reduce("Steve"));
        assertEquals("AAAAA", StringBinner.reduce("Stebe"));
        assertEquals("9999", StringBinner.reduce("1836"));
        assertEquals("99999", StringBinner.reduce("99344"));
        assertEquals("9999999", StringBinner.reduce("1234567"));
        assertEquals("9999999999", StringBinner.reduce("12345678"));
        assertEquals("9999999999", StringBinner.reduce("123456789"));
        assertEquals("9999999999999", StringBinner.reduce("1234567890"));
        assertEquals("9999999999999", StringBinner.reduce("12345678901"));
    }

    @Test
    public void shouldComplex() throws Exception {
        assertEquals("AAAA'AAAA", StringBinner.reduce("FDES'FRGD"));
        assertEquals("AAAA'AAAAA", StringBinner.reduce("FDES'FRGDF"));
        assertEquals("AAAA'AAAAA!", StringBinner.reduce("FDES'FRGDF!"));
        assertEquals("AAAA'AAAAA!9", StringBinner.reduce("FDES'FRGDF!7"));
        assertEquals("^%$&", StringBinner.reduce("^%$&"));
        assertEquals("", StringBinner.reduce("  "));
        assertEquals("A A", StringBinner.reduce("A  A"));
        assertEquals("A A", StringBinner.reduce("A   A"));
        assertEquals("AAAAAAAAAA'AAAAA!9", StringBinner.reduce("FDESFDSS'FRGDF!7"));
    }
}