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

package com.github.steveash.synthrec.domain;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;

/**
 * @author Steve Ash
 */
public class CompositeFeatureComputer implements FeatureComputer {

    private final ImmutableList<FeatureComputer> delegates;
    private final Set<FeatureKey<?>> sats;
    private final Set<FeatureKey<?>> reqs;

    public CompositeFeatureComputer(List<FeatureComputer> delegates) {
        this.delegates = ImmutableList.copyOf(delegates);
        this.sats = delegates.stream().flatMap(c -> c.satisfies().stream()).collect(toImmutableSet());
        this.reqs = delegates.stream().flatMap(c -> c.requires().stream()).collect(toImmutableSet());
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        for (FeatureComputer computer : delegates) {
            computer.emitFeatures(record, sink);
        }
    }

    @Override
    public Set<FeatureKey<?>> requires() {
        return reqs;
    }

    @Override
    public Set<FeatureKey<?>> satisfies() {
        return sats;
    }
}
