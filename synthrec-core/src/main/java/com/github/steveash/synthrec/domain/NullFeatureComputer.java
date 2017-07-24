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

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Noll object implementation
 * @author Steve Ash
 */
public class NullFeatureComputer implements FeatureComputer {

    public static final NullFeatureComputer INSTANCE = new NullFeatureComputer();

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        // no op
    }

    @Override
    public Set<FeatureKey<?>> requires() {
        return ImmutableSet.of();
    }

    @Override
    public Set<FeatureKey<?>> satisfies() {
        return ImmutableSet.of();
    }
}
