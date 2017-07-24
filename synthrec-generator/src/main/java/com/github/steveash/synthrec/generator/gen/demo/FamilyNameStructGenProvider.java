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

package com.github.steveash.synthrec.generator.gen.demo;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.generator.spring.LazyComponent;

/**
 * Structure gen node provider for family name
 * @author Steve Ash
 */
@LazyComponent
public class FamilyNameStructGenProvider extends BaseNameStructGenNodeProvider {

    public FamilyNameStructGenProvider() {
        super(Constants.FAMILY_NAME_STRUCT, Constants.FAMILY_NAME_CULTURE);
    }
}
