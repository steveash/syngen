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

package com.github.steveash.synthrec.collect;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

/**
 * Since guava deprecated their computing map (and it was concurrent anyways) and Apache commons collections aren't
 * Java5 yet -- this is a placeholder until one of those two libraries gets its act together.  I just want a simple
 * map that automatically inserts a default value into the map when it doesn't exist.
 * @author Steve Ash
 */
public class LazyMap<K,V> extends ForwardingMap<K,V> {

    public static <K,V> LazyMap<K,V> makeSupply(Supplier<V> valueFactory) {
        return make(dummy -> valueFactory.get());
    }

    public static <K,V> LazyMap<K,V> make(Function<K,V> valueFactory) {
        Map<K,V> map = Maps.newHashMap();
        return new LazyMap<>(map, valueFactory);
    }

    private final Map<K,V> delegate;
    private final Function<K,V> factory;

    private LazyMap(Map<K, V> delegate, Function<K, V> factory) {
        this.delegate = delegate;
        this.factory = factory;
    }

    @Override
    public V get(Object key) {
        V value = super.get(key);
        if (value == null) {
            value = factory.apply((K)key);
            delegate.put((K)key, value);
        }
        return value;
    }

    @Override
    public Map<K, V> delegate() {
        return delegate;
    }
}
