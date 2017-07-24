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

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.domain.AssignmentProvider;

/**
 * Interface for things that can generate a sample given a current assignment; the idea of course
 * being that the sampling process needs a grounding for the conditional variables so that it can sample
 * @author Steve Ash
 */
public interface ConditionalSampler<T> extends ISampler<T> {

    T sample(RandomGenerator rand, AssignmentProvider assignment);

    static <T> ConditionalSampler<T> adaptSampler(Sampler<T> unconditional) {
        return (rand, assignment) -> unconditional.sample(rand);
    }
}
