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

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;

import org.junit.Test;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.stat.Dists.ConditionalBuilder;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class MarginalizerTest {

    @Test
    public void shouldMarginalizeToOne() throws Exception {
        ConditionalBuilder<String> builder = Dists.condBuilder();
        builder.adderFor("FN","steve","LN", "ash")
                .add("a", 3)
                .add("b", 2)
                .incrementMany("c", "d");
        builder.adderFor("FN", "steve", "LN", "jones")
                .incrementMany("a", "b", "c", "d");
        builder.adderFor("FN","martha", "LN", "jones")
                .add("a", 2.0);

        Map<AssignmentInstance, MutableMultinomial<String>> result =
                Marginalizer.marginalizeTo(Sets.newHashSet("FN"), builder.getConditional());

        assertThat(result).hasSize(2);
        MutableMultinomial<String> steve = result.get(AssignmentInstance.make("FN", "steve"));
        MutableMultinomial<String> martha = result.get(AssignmentInstance.make("FN", "martha"));
        assertThat(steve.distrib).containsExactly("a", 4.0, "b", 3.0, "c", 2.0, "d", 2.0);
        assertThat(martha.distrib).containsExactly("a", 2.0);
    }
}