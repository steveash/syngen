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

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.stat.ConditionalSampler;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */ // this is a node that has a conditional -> joint distribution
public class ConditionalMultipleNode implements GenNode {

    private final Set<String> outputKeys;
    private final List<String> assignNames;
    private final Set<String> parent;
    private final ConditionalSampler<?> conditional;

    public ConditionalMultipleNode(List<String> assignNames,
            Set<String> parent,
            ConditionalSampler<?> conditional
    ) {
        this.outputKeys = ImmutableSet.copyOf(assignNames);
        this.assignNames = assignNames;
        this.parent = parent;
        this.conditional = conditional;
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        List<Object> sampledVal = (List<Object>) conditional.sample(rand, assignment);
        Preconditions.checkState(sampledVal.size() == assignNames.size(), "mismatched assign names vs value");
        for (int i = 0; i < assignNames.size(); i++) {
            assignment.put(assignNames.get(i), sampledVal.get(i));
        }
        return true;
    }

    @Override
    public Set<String> inputKeys() {
        return parent;
    }

    @Override
    public Set<String> outputKeys() {
        return outputKeys;
    }

    @Override
    public String toString() {
        return "ConditionalMultipleNode{" +
                "assignNames=" + assignNames +
                ", parent=" + parent +
                '}';
    }
}
