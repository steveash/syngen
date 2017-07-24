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

package com.github.steveash.synthrec.count;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.count.CountAssignment.Builder;
import com.github.steveash.synthrec.domain.MissingPolicy;
import com.github.steveash.synthrec.domain.Multivalue;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class CountAssignmentTest {
    private static final Logger log = LoggerFactory.getLogger(CountAssignmentTest.class);

    @Test
    public void shouldSubset() throws Exception {
        CountAssignment assign = CountAssignment.fromObserved(ImmutableMap.of("A", 1, "B", 2, "C", 3));
        Iterable<CountAssignmentEstimate> cas = assign.enumerateSubset(ImmutableSet.of("B", "C"),
                MissingPolicy.SKIP_WHOLE_RECORD
        );
        CountAssignmentEstimate est = Iterables.getFirst(cas, null);
        Assert.assertEquals(1, Iterables.size(cas));
        Assert.assertEquals(ImmutableMap.of("B", 2, "C", 3), est.getAssignmentMap());
    }

    @Test
    public void shouldIterMulti() throws Exception {
        CountAssignment assign = CountAssignment.fromObserved(ImmutableMap.of("A", 1, "B", 2, "C", new Multivalue<>(3, 4)));
        Iterable<CountAssignmentEstimate> cas = assign.enumerateSubset(ImmutableSet.of("B", "C"),
                MissingPolicy.SKIP_WHOLE_RECORD
        );
        List<String> results = Guavate.stream(cas)
                .map(CountAssignmentEstimate::toEntryString)
                .collect(Collectors.toList());
        assertThat(results).containsAllOf("B=2,C=3", "B=2,C=4");
    }

    @Test
    public void shouldIterMulti2() throws Exception {
        // 2 multis but one is not in the subset so shouldn't affect results
        CountAssignment assign = CountAssignment.fromObserved(ImmutableMap.of("A", new Multivalue<>(123, 456), "B", 2, "C", new Multivalue<>(3, 4)));
        Iterable<CountAssignmentEstimate> cas = assign.enumerateSubset(ImmutableSet.of("B", "C"),
                MissingPolicy.SKIP_WHOLE_RECORD
        );
        List<String> results = Guavate.stream(cas)
                .map(CountAssignmentEstimate::toEntryString)
                .collect(Collectors.toList());
        assertThat(results).containsAllOf("B=2,C=3", "B=2,C=4");
    }

    @Test
    public void shouldSubsetWithSkippedNulls() throws Exception {
        CountAssignment assign = CountAssignment.fromObserved(ImmutableMap.of("A", 1, "B", 2, "C", 3));
        Iterable<CountAssignmentEstimate> cas = assign.enumerateSubset(ImmutableSet.of("B", "C", "D"),
                MissingPolicy.SKIP_WHOLE_RECORD
        );
        CountAssignmentEstimate est = Iterables.getFirst(cas, null);
        Assert.assertNull(est);
    }

    @Test
    public void shouldSubsetWithNulls() throws Exception {
        CountAssignment assign = CountAssignment.fromObserved(ImmutableMap.of("A", 1, "B", 2, "C", 3));
        Iterable<CountAssignmentEstimate> cas = assign.enumerateSubset(ImmutableSet.of("B", "C", "D"),
                MissingPolicy.PLACEHOLDER
        );
        CountAssignmentEstimate est = Iterables.getFirst(cas, null);
        Assert.assertEquals(1, Iterables.size(cas));
        Assert.assertEquals(ImmutableMap.of("B", 2, "C", 3, "D", Constants.MISSING), est.getAssignmentMap());
    }

    @Test
    public void shouldSubsetEmpty() throws Exception {
        CountAssignment assign = CountAssignment.fromObserved(ImmutableMap.of("A", 1, "B", 2, "C", 3));
        Iterable<CountAssignmentEstimate> cas = assign.enumerateSubset(ImmutableSet.of(),
                MissingPolicy.SKIP_WHOLE_RECORD
        );
        CountAssignmentEstimate est = Iterables.getFirst(cas, null);
        Assert.assertNull(est);
    }

    @Test
    public void shouldEnumerateJoint1() throws Exception {
        Builder builder = CountAssignment.builder();
        builder.putObserved("A", "aa");
        builder.putObserved("B", "bb");
        MutableMultinomial<String> dens1 = new MutableMultinomial<>(4);
        dens1.add("c", 10);
        dens1.add("cc", 30);
        dens1.add("ccc", 60);
        builder.putUnobserved("C", dens1.normalize());
        CountAssignment assignment = builder.build();
        Iterable<CountAssignmentEstimate> estimates = assignment.enumerateSubset(ImmutableSet.of("A",
                "B",
                "C"
        ), MissingPolicy.SKIP_WHOLE_RECORD);
        HashSet<Object> collected = Sets.newHashSet();
        for (CountAssignmentEstimate estimate : estimates) {
            assertFalse(estimate.isFullyObserved());
            log.info("Got estimate " + estimate);
            String a = estimate.getAssignment().getString("A", null);
            String b = estimate.getAssignment().getString("B", null);
            String c = estimate.getAssignment().getString("C", null);
            collected.add(a + "," + b + "," + c);
            if (c.equals("ccc")) {
                assertEquals(0.6, estimate.certaintyOfVariable("C"), 0.01);
            } else if (c.equals("cc")) {
                assertEquals(0.3, estimate.certaintyOfVariable("C"), 0.01);
            } else if (c.equals("c")) {
                assertEquals(0.1, estimate.certaintyOfVariable("C"), 0.01);
            } else {
                fail();
            }
        }
        log.info("Got these isntances " + collected);
        assertEquals(3, collected.size());
    }

    @Test
    public void shouldEnumerateJoint2() throws Exception {
        Builder builder = CountAssignment.builder();
        builder.putObserved("A", "aa");
        MutableMultinomial<String> dens1 = new MutableMultinomial<>(4);
        dens1.add("c", 10);
        dens1.add("cc", 30);
        dens1.add("ccc", 60);
        builder.putUnobserved("C", dens1.normalize());
        MutableMultinomial<String> dens2 = new MutableMultinomial<>(4);
        dens2.add("b", 10);
        dens2.add("bb", 40);
        builder.putUnobserved("B", dens2.normalize());
        CountAssignment assignment = builder.build();
        Iterable<CountAssignmentEstimate> estimates = assignment.enumerateSubset(ImmutableSet.of("A",
                "B",
                "C"
        ), MissingPolicy.SKIP_WHOLE_RECORD);
        HashSet<Object> collected = Sets.newHashSet();
        for (CountAssignmentEstimate estimate : estimates) {
            assertFalse(estimate.isFullyObserved());
            log.info("Got estimate " + estimate);
            String a = estimate.getAssignment().getString("A", null);
            String b = estimate.getAssignment().getString("B", null);
            String c = estimate.getAssignment().getString("C", null);
            collected.add(a + "," + b + "," + c);
            if (c.equals("ccc")) {
                assertEquals(0.6, estimate.certaintyOfVariable("C"), 0.01);
            } else if (c.equals("cc")) {
                assertEquals(0.3, estimate.certaintyOfVariable("C"), 0.01);
            } else if (c.equals("c")) {
                assertEquals(0.1, estimate.certaintyOfVariable("C"), 0.01);
            } else {
                fail();
            }
        }
        log.info("Got these isntances " + collected);
        assertEquals(6, collected.size());
    }
}