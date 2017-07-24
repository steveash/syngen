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

package com.github.steveash.synthrec.util;

import java.util.Collection;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * @author Steve Ash
 */
public class MoreCollections {

    public static <K, V> ImmutableMap<K, V> subsetMap(Map<K, V> source,
            Collection<K> keysToSelect
    ) {
        if (keysToSelect.isEmpty()) {
            return ImmutableMap.of();
        }
        Builder<K, V> builder = ImmutableMap.builder();
        for (K key : keysToSelect) {
            V val = source.get(key);
            Preconditions.checkNotNull(val, "Cant subset keys %s from %s", keysToSelect, source);
            builder.put(key, val);
        }
        return builder.build();
    }
}
