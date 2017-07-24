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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Simple base class for the common case of 1 in and 1 out
 * @author Steve Ash
 */
public abstract class InOutGenNode implements GenNode {

    private final ImmutableSet<String> inputKeys;
    private final ImmutableSet<String> outputKey;

    /**
     *
     * @param keys the first n - 1 keys are the inputs and the last is the output key
     */
    protected InOutGenNode(String... keys) {
        Preconditions.checkArgument(keys.length >= 1, "must pass at least an output key");
        List<String> keyList = Arrays.asList(keys);
        this.inputKeys = ImmutableSet.copyOf(keyList.subList(0, keyList.size() - 1));
        this.outputKey = ImmutableSet.of(keyList.get(keyList.size() - 1));
    }

    @Override
    public final Set<String> inputKeys() {
        return inputKeys;
    }

    @Override
    public final Set<String> outputKeys() {
        return outputKey;
    }
}
