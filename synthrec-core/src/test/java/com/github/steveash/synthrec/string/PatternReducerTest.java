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

/**
 * @author Steve Ash
 */
public class PatternReducerTest {

    @Test
    public void shouldPreserveEscapes() throws Exception {
        assertEquals("999-{{456}}-999", PatternReducer.replace("123-{{456}}-789"));
        assertEquals("999-{{456}}{{789}}-999", PatternReducer.replace("123-{{456}}{{789}}-789"));
        assertEquals("999-{{45{{6}}}}{{789}}-999", PatternReducer.replace("123-{{45{{6}}}}{{789}}-789"));
        assertEquals("999-{{45{{6}}}}AAA{{789}}-999", PatternReducer.replace("123-{{45{{6}}}}bcv{{789}}-789"));
    }

    @Test
    public void shouldSimple() throws Exception {
        assertEquals("999-999-9999", PatternReducer.replace(" 403-000-3244 "));
        assertEquals("999-999-9999", PatternReducer.replace(" 403-000-3244 \n "));
        assertEquals("999-999-9999", PatternReducer.replace(" 403-000 - 3244 \n "));
        assertEquals("999-999-9999", PatternReducer.replace(" 403-000 -3244"));
        assertEquals("999-999-9999", PatternReducer.replace(" 403-000- 3244"));
    }

    @Test
    public void shouldRetainMultiLeadZeroes() throws Exception {
        assertEquals("009-999-9999", PatternReducer.replace(" 004-000-3244 "));
        assertEquals("999-999-9999", PatternReducer.replace(" 044-000-3244 "));
        assertEquals("999-999-9999", PatternReducer.replace(" 044-000-3240 "));
        assertEquals("999-999-9900", PatternReducer.replace(" 044-000-3200 "));
        assertEquals("000-999-0000", PatternReducer.replace(" 000-909-0000 "));
        assertEquals("000-000-0000", PatternReducer.replace(" 000-000-0000 "));
        assertEquals("0000000000", PatternReducer.replace(" 0000000000 "));

        // skip punc in the calc
        assertEquals("-999-9999", PatternReducer.replace("-304-0943"));
        assertEquals("-999-9999", PatternReducer.replace("-034-0943"));
        assertEquals("-009-9999", PatternReducer.replace("-004-0943"));
        assertEquals("-000-0999", PatternReducer.replace("-000-0943"));
        assertEquals("-000-0099", PatternReducer.replace("-000-0043"));
        assertEquals("-000-009999", PatternReducer.replace("-000-004043"));
        assertEquals("-000-009999-", PatternReducer.replace("-000-004040-"));
        assertEquals("-000-009990-0", PatternReducer.replace("-000-004040-0"));
        assertEquals("-000-009990-00", PatternReducer.replace("-000-004040-00"));
        assertEquals("-000-009999-99", PatternReducer.replace("-000-004040-04"));
    }
}