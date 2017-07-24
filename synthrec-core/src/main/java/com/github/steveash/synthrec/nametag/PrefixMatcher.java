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

package com.github.steveash.synthrec.nametag;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.left;

import java.util.Collection;
import java.util.Set;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class PrefixMatcher {

    private final Set<String> matches;
    private final int minLen;
    private final int maxLen;

    public PrefixMatcher(Iterable<String> matches) {
        Set<String> results = Sets.newHashSet();
        int min = -1;
        int max = -1;
        for (String match : matches) {
            if (isBlank(match)) {
                continue;
            }
            results.add(normalize(match));
            if (min == -1) {
                min = match.length();
                max = match.length();
            } else {
                min = Math.min(min, match.length());
                max = Math.max(max, match.length());
            }
        }
        this.matches = ImmutableSet.copyOf(results);
        this.minLen = min;
        this.maxLen = max;
    }

    private String normalize(String match) {return match.toLowerCase();}

    public boolean matches(String candidate) {
        if (minLen == -1) {
            return false;
        }
        int maxToCheck = Math.min(maxLen, candidate.length());
        for (int i = minLen; i <= maxToCheck; i++) {
            if (matches.contains(normalize(left(candidate, i)))) {
                return true;
            }
        }
        return false;
    }
}
