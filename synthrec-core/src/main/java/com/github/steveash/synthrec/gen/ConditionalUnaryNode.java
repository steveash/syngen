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

import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.stat.ConditionalSampler;
import com.google.common.collect.ImmutableSet;

/**
 * Gen node that is a conditional distribution to a single value
 * @author Steve Ash
 */
public class ConditionalUnaryNode implements GenNode {

    private final String assignName;
    private final Set<String> parent;
    private final ConditionalSampler<?> conditional;

    public ConditionalUnaryNode(String assignName,
            Set<String> parent,
            ConditionalSampler<?> conditional
    ) {
        this.assignName = assignName;
        this.parent = parent;
        this.conditional = conditional;
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        Object sample = conditional.sample(rand, assignment);
        assignment.put(assignName, sample);
        return true;
    }

    @Override
    public Set<String> inputKeys() {
        return parent;
    }

    @Override
    public Set<String> outputKeys() {
        return ImmutableSet.of(assignName);
    }

    @Override
    public String toString() {
        return "ConditionalUnaryNode{" +
                "assignName='" + assignName + '\'' +
                ", parent=" + parent +
                '}';
    }
}
