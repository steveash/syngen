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

package com.github.steveash.synthrec.name;

/**
 * The data for all public names; you provide your own sources of what is a public name (from SSA, your own
 * dictionaries, etc.) and to use the built in features, provide an implementation of this
 * @author Steve Ash
 */
public interface GivenNameLookup {

    /**
     * Return all names normalized by Names#normalize
     * @see Names#normalize(String)
     * @return
     */
    Iterable<String> allNames();

    /**
     * Returns true if this name is a public token and would not comporomise privacy (because it is generally known
     * to be a name and thus no additional information is disclosed by the token)
     * @param normalName you can assume that the name will be normalized
     * @return
     */
    boolean isPublicName(String normalName);
}
