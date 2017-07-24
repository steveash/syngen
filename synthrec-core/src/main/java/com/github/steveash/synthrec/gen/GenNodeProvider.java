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

import com.github.steveash.synthrec.count.CountDag;
import com.google.common.collect.ImmutableSet;

/**
 * Some gen nodes dont need any profiling information and thus they can just be singletons, relying only on
 * real-world semantics.  Some genNodes are based on information from the sapmling session and thus must be
 * constructed _given_ the profiling information (i.e. a CountDag)
 * @author Steve Ash
 */
public interface GenNodeProvider {

    /**
     * Construct a GenNode instance for the given name using the supplied CountDag profiling information
     * @param name node to generate for
     * @param countDag the count dag instance
     * @return
     */
    GenNode makeFor(String name, CountDag countDag);

    /**
     * The set of names that this factory can provide for; note that any names returned by all providers
     * should _not_ be available as singleton/container GenNode instances
     * @return
     */
    Set<String> providesForNames();
}
