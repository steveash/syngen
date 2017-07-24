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

package com.github.steveash.synthrec.gen;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

/**
 * Context shared by the entire generation process; this is where genreators can store info about
 * what they've already produced in order to do thing like ensure uniqueness
 * @author Steve Ash
 */
public class GenContext {

    public static final class ContextKey<T> {
        private final String key;
        private final Supplier<T> defaultValueFactory;

        public ContextKey(String key, Supplier<T> defaultValueFactory) {
            this.key = key;
            this.defaultValueFactory = defaultValueFactory;
        }
    }

    private final Map<String, Object> context = Maps.newHashMap();
    private final int recordCountToCreate;

    public GenContext(int recordCountToCreate) {this.recordCountToCreate = recordCountToCreate;}

    public <T> T get(ContextKey<T> key) {
        return (T) context.computeIfAbsent(key.key, k -> key.defaultValueFactory.get());
    }
}
