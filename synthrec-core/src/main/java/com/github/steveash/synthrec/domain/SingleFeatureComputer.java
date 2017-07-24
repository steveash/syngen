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

import java.util.Arrays;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Common feature computer case is one that emits a single feature and requires maybe other features to have already
 * been computed
 * @author Steve Ash
 */
public abstract class SingleFeatureComputer implements FeatureComputer {

    private final ImmutableSet<FeatureKey<?>> satisfyKeys;
    private final ImmutableSet<FeatureKey<?>> requireKeys;

    public SingleFeatureComputer(FeatureKey<?> singleOutputKey, FeatureKey<?>... requireKeys) {
        this.satisfyKeys = ImmutableSet.of(singleOutputKey);
        this.requireKeys = ImmutableSet.copyOf(Arrays.asList(requireKeys));
    }

    @Override
    public final Set<FeatureKey<?>> requires() {
        return requireKeys;
    }

    @Override
    public final Set<FeatureKey<?>> satisfies() {
        return satisfyKeys;
    }
}
