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

package com.github.steveash.synthrec.deident;

import java.util.Set;

/**
 * Abstraction for converting a value into a vector that represents this value (maybe based on multiple
 * dimensions). And owns the measurement strategy
 * @author Steve Ash
 */
public interface DeidentDistance<I, V> {

    /**
     * Makes a sketch of the actual input value (this might be transformed or have ancillary info that
     * you dont want to recalculate all the time)
     * @param input
     * @return
     */
    V makeVector(I input);

    /**
     * There are plenty of inputs that intrinsically are not sensitive and don't need deident
     * @param input
     * @return
     */
    boolean isPublicDomain(I input);

    /**
     * Distance between two sketches
     * @param comp1
     * @param comp2
     * @return
     */
    double distance(V comp1, V comp2);

    /**
     * @param input value to generate blocking keys for
     * @return
     */
    Set<String> blockingKeys(I input);
}
