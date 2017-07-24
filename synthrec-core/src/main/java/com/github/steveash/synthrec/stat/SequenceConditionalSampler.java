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

import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.google.common.collect.ImmutableList;

/**
 * A kind of sampler that returns a sequence of values based on a requested length; the returned
 * sequence might NOT be the requested length but will never be more than the requested length
 * @author Steve Ash
 */
public interface SequenceConditionalSampler<T> extends ISampler<T> {

    List<T> sample(RandomGenerator rand, int requestedCount, AssignmentInstance conditionedOn);

    static <T> SequenceConditionalSampler<T> adaptFrom(Sampler<T> delegate) {
        return (r, c, a) -> ImmutableList.of(delegate.sample(r));
    }

    static <T> SequenceConditionalSampler<T> adaptFrom(ConditionalSampler<T> delegate) {
        return (r, c, a) -> ImmutableList.of(delegate.sample(r, a));
    }
}
