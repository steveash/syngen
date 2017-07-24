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

package com.github.steveash.synthrec.name.culture;

import com.github.steveash.synthrec.canonical.NormalToken;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;

/**
 * A null object implementation of culturedetector that just returns unknown for everything
 * @author Steve Ash
 */
public class NullCultureDetector implements CultureDetector {

    public static final String UNKNOWN = "UNKNOWN_CULTURE";
    private static final Multinomial<String> UNKNOWN_DIST = MutableMultinomial
            .makeNormalizedFrom(UNKNOWN)
            .toImmutable();

    @Override
    public Multinomial<String> detectSingleToken(NormalToken name) {
        return UNKNOWN_DIST;
    }
}
