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

package com.github.steveash.synthrec.domain;

import static com.google.common.truth.Truth.assertThat;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @author Steve Ash
 */
public class MultivalueIterableTest {

    @Test
    public void shouldTestOne() throws Exception {
        Iterator<Map<String, Object>> iter = MultivalueIterable.enumerate(ImmutableMap.of("FN", "Steve")).iterator();
        assertThat(iter.hasNext()).isTrue();
        Map<String, Object> res0 = iter.next();
        assertThat(res0).containsExactly("FN", "Steve");
        assertThat(iter.hasNext()).isFalse();
    }

    @Test
    public void shouldTestOneMulti() throws Exception {
        Iterator<Map<String, Object>> iter = MultivalueIterable.enumerate(ImmutableMap.of(
                "FN", "Steve", "LN", new Multivalue("Ash", "Bash"))).iterator();
        assertThat(iter.hasNext()).isTrue();
        Map<String, Object> res0 = iter.next();
        assertThat(res0).containsExactly("FN", "Steve", "LN", "Ash");
        Map<String, Object> res1 = iter.next();
        assertThat(res1).containsExactly("FN", "Steve", "LN", "Bash");
        assertThat(iter.hasNext()).isFalse();
    }

    @Test
    public void shouldTestTwoMulti() throws Exception {
        Iterator<Map<String, Object>> iter = MultivalueIterable.enumerate(ImmutableMap.of(
                "FN", "Steve",
                "LN", new Multivalue("Ash", "Bash"),
                "SX", new Multivalue("JR", "SR")
        )).iterator();
        assertThat(iter.hasNext()).isTrue();
        Map<String, Object> res0 = iter.next();
        assertThat(res0).containsExactly("FN", "Steve", "LN", "Ash", "SX", "JR");
        Map<String, Object> res1 = iter.next();
        assertThat(res1).containsExactly("FN", "Steve", "LN", "Bash", "SX", "JR");
        Map<String, Object> res2 = iter.next();
        assertThat(res2).containsExactly("FN", "Steve", "LN", "Ash", "SX", "SR");
        Map<String, Object> res3 = iter.next();
        assertThat(res3).containsExactly("FN", "Steve", "LN", "Bash", "SX", "SR");
        assertThat(iter.hasNext()).isFalse();
    }
}