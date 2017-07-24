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

import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
public class MultipleNode implements GenNode {

    private final Set<String> outputKeys;
    private final List<String> assignName;
    private final SamplingTable<List<Object>> unconditional;

    public MultipleNode(List<String> assignName,
            SamplingTable<List<Object>> unconditional
    ) {
        this.assignName = assignName;
        this.outputKeys = ImmutableSet.copyOf(assignName);
        this.unconditional = unconditional;
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        List<Object> sampledVal = unconditional.sampleWeighted(rand);
        Preconditions.checkNotNull(sampledVal);
        Preconditions.checkState(sampledVal.size() == assignName.size(), "mistmatch assign name vs sampled");
        for (int i = 0; i < assignName.size(); i++) {
            assignment.put(assignName.get(i), sampledVal.get(i));
        }
        return true;
    }

    @Override
    public Set<String> outputKeys() {
        return outputKeys;
    }
}
