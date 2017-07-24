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

package com.github.steveash.synthrec.reducer;

import com.github.steveash.synthrec.util.Object2Double;

/**
 * A reducer knows how to inspect particular values for profiling and recognize what is a real token
 * vs something that is a string of random alphanumerics in which case we don't want to profile the
 * actual string, but instead profiling the _pattern_ then at generation time, resample/generate a
 * new random string
 *
 * For example when profiling "apartment number" the reducer is responsible for the semantics of
 * recognizing that in "APT 12B" the APT is a token that has meaning and the 12B is 2 random digits + a random
 * letter and we should count that fact instead.  Because frequency and dictionary lookups are often the only
 * good way we have to determine this systematically this gets the counted frequency as an input
 *
 * Reducers own the semantics but can be used in a few places; their primary use though is in CountDagService
 * _after_ profiling/counting all of the fields
 * @author Steve Ash
 */
public interface ValueReducer {

    /**
     * Reduce this value if its needed and return the reduced value; note that this return value
     * is likely needed to be tagged so that you can recognize it later
     * @see TaggedValue
     * @param input
     * @param empiricalCount the count of the item or -1 if you dont know the count
     * @return
     */
    String reduceIfNecessary(String input, double empiricalCount);

    /**
     * Call this when you want to reduce the value but do not know the  count of the item yet
     * @param input
     * @return
     */
    default String reduceIfNecessary(String input) {
        return reduceIfNecessary(input, -1);
    }
}
