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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
public class StringEscaperTest {

    private StringEscaper escaper;

    @Before
    public void setUp() throws Exception {
        escaper = new StringEscaper(ImmutableSet.of("ext", "dog", "cat"));
    }

    @Test
    public void shouldEscape() throws Exception {
        assertThat(escaper.escape("steve")).isEqualTo("steve");
        assertThat(escaper.escape("phoneext")).isEqualTo("phone{{ext}}");
        assertEquals("{{ext}}phone", escaper.escape("extphone"));
        assertEquals("phone{{ext}}phone", escaper.escape("phoneextphone"));
        assertEquals("phone{{dog}}phone", escaper.escape("phonedogphone"));
        assertEquals("phone{{DoG}}phone", escaper.escape("phoneDoGphone"));
        assertThat(escaper.escape("")).isEqualTo("");
    }
}