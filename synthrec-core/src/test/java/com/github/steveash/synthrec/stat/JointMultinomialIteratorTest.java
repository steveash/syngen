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

package com.github.steveash.synthrec.stat;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.github.steveash.synthrec.stat.JointDensityIterator.JointEntry;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class JointMultinomialIteratorTest {
    public static final Joiner COMMA = Joiner.on(',');

    @Test
    public void shouldIterOnlyOne() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A"), make("B"), make("C"));
        assertShould(iter, entry("A", "B", "C"));
    }
    @Test
    public void shouldIterOnlyOne2() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A"));
        assertShould(iter, entry("A"));
    }
    @Test
    public void shouldIterOnlyOneTwo() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A"), make("B", "BB"), make("C"));
        assertShould(iter, entry("A", "B", "C"), entry("A", "BB", "C"));
    }
    @Test
    public void shouldIterOnlyOneTwo2() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A", "AA"), make("B"), make("C"));
        assertShould(iter, entry("A", "B", "C"), entry("AA", "B", "C"));
    }
    @Test
    public void shouldIterOnlyOneTwo3() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A"), make("B"), make("C", "CC"));
        assertShould(iter, entry("A", "B", "C"), entry("A", "B", "CC"));
    }
    @Test
    public void shouldIterOnlyTwoTwo() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A"), make("B", "BB"), make("C", "CC"));
        assertShould(iter, entry("A", "B", "C"),
                entry("A", "B", "CC"),
                entry("A", "BB", "C"),
                entry("A", "BB", "CC")
        );
    }
    @Test
    public void shouldIterOnlyTwoTwo2() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A", "AA"), make("B", "BB"), make("C"));
        assertShould(iter, entry("A", "B", "C"),
                entry("AA", "B", "C"),
                entry("A", "BB", "C"),
                entry("AA", "BB", "C")
        );
    }
    @Test
    public void shouldIterOnlyThree() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A", "AA", "AAA"), make("B"), make("C"));
        assertShould(iter, entry("A", "B", "C"),
                entry("AA", "B", "C"),
                entry("AAA", "B", "C")
        );
    }
    @Test
    public void shouldIterOnlySix() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A", "AA", "AAA"), make("B", "BB"), make("C"));
        assertShould(iter, entry("A", "B", "C"),
                entry("AA", "B", "C"),
                entry("AAA", "B", "C"),
                entry("A", "BB", "C"),
                entry("AA", "BB", "C"),
                entry("AAA", "BB", "C")
        );
    }
    @Test
    public void shouldIterOnlyNine() throws Exception {
        JointDensityIterator iter = new JointDensityIterator(make("A", "AA", "AAA"), make("B", "BB", "BBB"), make("C"));
        assertShould(iter, entry("A", "B", "C"),
                entry("AA", "B", "C"),
                entry("AAA", "B", "C"),
                entry("A", "BB", "C"),
                entry("AA", "BB", "C"),
                entry("AAA", "BB", "C"),
                entry("A", "BBB", "C"),
                entry("AA", "BBB", "C"),
                entry("AAA", "BBB", "C")
        );
    }

    private void assertShould(JointDensityIterator iter, Object[]... expectedEntries) {
        Set<String> expected = Sets.newHashSet();
        Set<String> actual = Sets.newHashSet();
        for (Object[] entry : expectedEntries) {
            expected.add(COMMA.join(entry));
        }
        while (iter.hasNext()) {
            JointEntry next = iter.next();
            actual.add(COMMA.join(next.entries()));
        }
        assertEquals(expected, actual);
    }

    private Set<Object[]> pull(JointDensityIterator iter) {
        HashSet<Object[]> outs = Sets.newHashSet();
        while (iter.hasNext()) {
            outs.add(iter.next().entries());
        }
        return outs;
    }

    private Object[] entry(String... elems) {
        return elems;
    }

    private Multinomial<String> make(String... elems) {
        MutableMultinomial<String> dens = new MutableMultinomial<>(elems.length);
        for (String elem : elems) {
            dens.set(elem, 1.0);
        }
        return dens;
    }
}