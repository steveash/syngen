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

package com.github.steveash.synthrec.generator.reducer;

import javax.annotation.Resource;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.reducer.ValueReducer;
import com.github.steveash.synthrec.reducer.ValueReducerRegistry;

/**
 * If there is a reducer for this demographic field, then it returns it
 * @author Steve Ash
 */
@LazyComponent
public class SpringValueReducerRegistry implements ValueReducerRegistry {

    @Resource private SimpleTokenReducer addressTokenReducer;
    @Resource private SimpleTokenReducer nameTokenReducer;

    @Override
    public ValueReducer reducerFor(String field) {
        switch (field) {
            case Constants.ADDRESS_STREET_STRUCT:
            case Constants.ADDRESS_STREET_NO:
                return addressTokenReducer;
            case Constants.GIVEN_NAME_STRUCT:
            case Constants.FAMILY_NAME_STRUCT:
            case Constants.GIVEN_NAMEISH:
            case Constants.FAMILY_NAMEISH:
                return nameTokenReducer;
            default:
                throw new IllegalArgumentException("no value reducer for field " + field);
        }
    }
}
