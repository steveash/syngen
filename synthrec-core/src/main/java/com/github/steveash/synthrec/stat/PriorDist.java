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

/**
 * segregated intrerface for a background/prior
 * @author Steve Ash
 */
public interface PriorDist<T> {

    /**
     * Returns the count from the prior of this input or zero if there is no count information from the prior
     * for this particular input
     * @param input
     * @return
     */
    double countFor(T input);

    /**
     * Returns all of the keys -- this should be generally unique but doesn't have to be; however if there
     * are duplicates then countFor must return the same value for the same input if called more than once
     * @return
     */
    Iterable<T> allKeys();


}
