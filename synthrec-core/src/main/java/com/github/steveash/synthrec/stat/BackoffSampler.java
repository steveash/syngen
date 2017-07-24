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

import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.AssignmentProvider;
import com.google.common.base.Preconditions;

/**
 * When you to sample from a conditional distribution, but you still need to backoff to a
 * unconditinoal distribution
 * @author Steve Ash
 */
public class BackoffSampler<T> implements ConditionalSampler<T> {

    private Map<AssignmentInstance, ? extends Sampler<T>> conditional;
    private ConditionalSampler<T> backoff;
    private final Set<String> parents;

    public BackoffSampler(Map<AssignmentInstance, ? extends Sampler<T>> conditional,
            Sampler<T> backoff,
            Set<String> parents
    ) {
        this(conditional, ConditionalSampler.adaptSampler(backoff), parents);
    }

    public BackoffSampler(Map<AssignmentInstance, ? extends Sampler<T>> conditional,
            ConditionalSampler<T> backoff,
            Set<String> parents
    ) {
        this.conditional = conditional;
        this.backoff = backoff;
        this.parents = parents;
    }

    @Override
    public T sample(RandomGenerator rand, AssignmentProvider currentAssigment) {
        AssignmentInstance parentAssign = currentAssigment.subset(parents);
        Sampler<T> dist = conditional.get(parentAssign);
        T sampledVal;
        if (dist != null && dist != VoidSampler.getInstance()) {
            sampledVal = dist.sample(rand);
        } else {
            sampledVal = backoff.sample(rand, currentAssigment);
        }
        return Preconditions.checkNotNull(sampledVal, "cant sample null");
    }
}
