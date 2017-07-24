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

package com.github.steveash.synthrec.gen;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * A node in the generator flow (its a DAG)
 * @author Steve Ash
 */
public interface GenNode {

    static String chainToString(List<GenNode> chain) {
        return chain.stream()
                .map(GenNode::nodeToString)
                .collect(Collectors.joining(","));
    }

    static String nodeToString(GenNode gn) {
        if (gn.outputKeys().size() == 1) {
            return Iterables.getFirst(gn.outputKeys(), null);
        } else {
            return "(" + gn.outputKeys().stream().collect(Collectors.joining(",")) + ")";
        }
    }

    boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context);

    /**
     * @return the keys that this node needs to be present in the assignment to geneate things
     */
    default Set<String> inputKeys() {
        return ImmutableSet.of();
    }

    /**
     * @return the keys that this node will output into the assignment
     */
    Set<String> outputKeys();
}
