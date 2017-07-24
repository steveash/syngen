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

import com.google.common.collect.ImmutableList;

/**
 * A sampler that randomly delegates to a set of other samplers
 * @author Steve Ash
 */
public class CompositeSampler<T> implements Sampler<T> {

    private final ImmutableList<Sampler<T>> delegates;

    public CompositeSampler(Iterable<? extends Sampler<T>> delegates) {this.delegates = ImmutableList.copyOf(delegates);}

    @Override
    public T sample(RandomGenerator rand) {
        int idx = rand.nextInt(delegates.size());
        return delegates.get(idx).sample(rand);
    }
}
