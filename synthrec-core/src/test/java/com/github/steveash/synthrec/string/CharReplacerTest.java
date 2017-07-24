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

import static com.github.steveash.synthrec.string.CharReplacer.replaceConsecutiveAlpha;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ash
 */
public class CharReplacerTest {

    private RandomGenerator mock;

    @Before
    public void setUp() throws Exception {
        this.mock = mock(RandomGenerator.class);
        when(mock.nextInt(anyInt())).thenReturn(25); // which will map to Z
    }

    @Test
    public void shouldReplace() throws Exception {
        assertThat(replaceConsecutiveAlpha(mock, "123456789", 0)).isEqualTo("123456789");
        assertThat(replaceConsecutiveAlpha(mock, "1234a6789", 0)).isEqualTo("1234Z6789");
        assertThat(replaceConsecutiveAlpha(mock, "1234aa6789", 0)).isEqualTo("1234ZZ6789");
        assertThat(replaceConsecutiveAlpha(mock, "a123456789", 0)).isEqualTo("Z123456789");
        assertThat(replaceConsecutiveAlpha(mock, "aa123456789", 0)).isEqualTo("ZZ123456789");
        assertThat(replaceConsecutiveAlpha(mock, "123456789a", 0)).isEqualTo("123456789Z");
        assertThat(replaceConsecutiveAlpha(mock, "123456789aa", 0)).isEqualTo("123456789ZZ");
        assertThat(replaceConsecutiveAlpha(mock, "abc", 0)).isEqualTo("ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "1abc", 0)).isEqualTo("1ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "11abc", 0)).isEqualTo("11ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "abc1", 0)).isEqualTo("ZZZ1");
        assertThat(replaceConsecutiveAlpha(mock, "abc11", 0)).isEqualTo("ZZZ11");
        assertThat(replaceConsecutiveAlpha(mock, "a1a", 0)).isEqualTo("Z1Z");
        assertThat(replaceConsecutiveAlpha(mock, "aa1aa", 0)).isEqualTo("ZZ1ZZ");
        assertThat(replaceConsecutiveAlpha(mock, "1a1a1", 0)).isEqualTo("1Z1Z1");
        assertThat(replaceConsecutiveAlpha(mock, "a1a1a", 0)).isEqualTo("Z1Z1Z");
        assertThat(replaceConsecutiveAlpha(mock, "aaaaa", 0)).isEqualTo("ZZZZZ");

        assertThat(replaceConsecutiveAlpha(mock, "abc", 1)).isEqualTo("ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "1abc", 1)).isEqualTo("1ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "11abc", 1)).isEqualTo("11ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "abc1", 1)).isEqualTo("ZZZ1");
        assertThat(replaceConsecutiveAlpha(mock, "abc11", 1)).isEqualTo("ZZZ11");
        assertThat(replaceConsecutiveAlpha(mock, "a1a", 1)).isEqualTo("Z1Z");
        assertThat(replaceConsecutiveAlpha(mock, "aa1aa", 1)).isEqualTo("ZZ1ZZ");

        assertThat(replaceConsecutiveAlpha(mock, "a1aa1a", 2)).isEqualTo("a1ZZ1a");
        assertThat(replaceConsecutiveAlpha(mock, "a1aa1aa", 2)).isEqualTo("a1ZZ1ZZ");
        assertThat(replaceConsecutiveAlpha(mock, "a1a1aa", 2)).isEqualTo("a1a1ZZ");
        assertThat(replaceConsecutiveAlpha(mock, "aa1a1aa", 2)).isEqualTo("ZZ1a1ZZ");

        assertThat(replaceConsecutiveAlpha(mock, "aaa", 3)).isEqualTo("ZZZ");
        assertThat(replaceConsecutiveAlpha(mock, "aaa", 4)).isEqualTo("aaa");
        assertThat(replaceConsecutiveAlpha(mock, "aaa", 5)).isEqualTo("aaa");
    }
}