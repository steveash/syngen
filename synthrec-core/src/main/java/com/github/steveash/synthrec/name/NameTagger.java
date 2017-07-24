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

import java.util.List;

/**
 * Knows how to tokenize and parse the tokens of a name to construct a PersonalName
 * instance
 * @author Steve Ash
 */
public interface NameTagger {

    /**
     * Takes a list of input segments (like FirstName, MiddleName, LastName) and produces a
     * parsed PersonalName which allows access to all tokens + their assigned semantic labels
     * @param fields
     * @return
     */
    PersonalName parse(List<InputField> fields);
}
