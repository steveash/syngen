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

import java.util.Set;
import java.util.function.Function;

import org.junit.Test;

import com.github.steveash.synthrec.nametag.MultiMatcher.ListTokenSeq;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class MultiMatcherTest {

    @Test
    public void shouldMatch() throws Exception {
        MultiMatcher matcher = MultiMatcher.makeFrom(Lists.newArrayList(
                ImmutableList.of("steve", "ash", "you"),
                ImmutableList.of("steve", "ash"),
                ImmutableList.of("steve", "bash", "third"),
                ImmutableList.of("sam")
        ), Function.identity());
        assertEquals(-1, matcher.prefixMatches(ListTokenSeq.make("steve")));
        assertEquals(-1, matcher.prefixMatches(ListTokenSeq.make("steve", "stash")));
        assertEquals(-1, matcher.prefixMatches(ListTokenSeq.make("steve", "stash", "buddy")));
        assertEquals(-1, matcher.prefixMatches(ListTokenSeq.make("steve", "bash", "yellow")));

        assertEquals(3, matcher.prefixMatches(ListTokenSeq.make("steve", "bash", "third")));
        assertEquals(3, matcher.prefixMatches(ListTokenSeq.make("steve", "bash", "third", "me")));
        assertEquals(3, matcher.prefixMatches(ListTokenSeq.make("steve", "bash", "third", "me", "them")));
        assertEquals(2, matcher.prefixMatches(ListTokenSeq.make("steve", "ash")));
        assertEquals(2, matcher.prefixMatches(ListTokenSeq.make("steve", "ash", "bob")));
        assertEquals(3, matcher.prefixMatches(ListTokenSeq.make("steve", "ash", "you")));
        assertEquals(3, matcher.prefixMatches(ListTokenSeq.make("steve", "ash", "you", "them")));
        assertEquals(1, matcher.prefixMatches(ListTokenSeq.make("sam")));
        assertEquals(1, matcher.prefixMatches(ListTokenSeq.make("sam", "smith")));
        assertEquals(1, matcher.prefixMatches(ListTokenSeq.make("sam", "smith", "third")));
    }

    @Test
    public void shouldIterateAllTokens() throws Exception {
        MultiMatcher matcher = MultiMatcher.makeFrom(Lists.newArrayList(
                ImmutableList.of("a", "b", "c", "d"),
                ImmutableList.of("e", "f"),
                ImmutableList.of("g"),
                ImmutableList.of("h"),
                ImmutableList.of("i")
        ), Function.identity());
        Set<String> expected = ImmutableSet.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        Set<String> actual = Sets.newHashSet(matcher.allTokens());
        assertEquals(expected, actual);
    }
}