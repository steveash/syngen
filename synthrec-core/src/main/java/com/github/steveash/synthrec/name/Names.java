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

import static com.github.steveash.synthrec.canonical.Normalizers.interner;
import static com.github.steveash.synthrec.canonical.Normalizers.rawToStandard;
import static com.github.steveash.synthrec.canonical.Normalizers.standardToName;
import static com.github.steveash.synthrec.data.ReadWrite.linesFrom;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableSet;

/**
 * General utilities around names, normalization, etc.
 * @author Steve Ash
 */
public class Names {

    public static final int MIN_PHONETIC_KEY = 3;

    public static String normalizeIntern(String raw) {
        return interner().intern(normalize(raw));
    }

    public static String normalize(String raw) {
        return standardToName().normalize(rawToStandard().normalize(raw));
    }

    public static ImmutableSet<String> loadNameTextData(String resourceName) {
        return loadNameTextData(resourceName, -1);
    }

    public static ImmutableSet<String> loadNameTextData(
            String resourceName,
            int limit
    ) {
        long limiter = Long.MAX_VALUE;
        if (limit > 0) {
            limiter = limit;
        }
        return linesFrom(resourceName)
                .map(Names::normalizeIntern)
                .limit(limiter)
                .collect(Guavate.toImmutableSet());
    }
}
