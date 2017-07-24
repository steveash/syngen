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

package com.github.steveash.synthrec.data;

import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

/**
 * Common global jvm cache for things that might or might not be loaded multiple times
 * (maybe i shouldve just gone with DI throughout)
 * @author Steve Ash
 */
public class Cacher {

    private static final ConcurrentMap<String,Object> cache = Maps.newConcurrentMap();

    public static <T> T get(String name, Supplier<T> loader) {
        return (T) cache.computeIfAbsent(name, func -> loader.get());
    }
}
