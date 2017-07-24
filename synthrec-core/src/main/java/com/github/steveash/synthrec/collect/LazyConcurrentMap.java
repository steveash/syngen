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

import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingConcurrentMap;
import com.google.common.collect.Maps;

/**
 * A concurrent lazy calculating map
 */
public class LazyConcurrentMap<K, V> extends ForwardingConcurrentMap<K, V> {

    public static <K, V> LazyConcurrentMap<K, V> make(Function<K, V> defaultValueFactory) {
        ConcurrentMap<K, V> map = Maps.newConcurrentMap();
        return new LazyConcurrentMap<>(map, defaultValueFactory);
    }

    public static <K, V> LazyConcurrentMap<K, V> wrap(ConcurrentMap<K, V> delegate, Function<K, V> defaultValueFactory) {
        return new LazyConcurrentMap<K, V>(delegate, defaultValueFactory);
    }

    private final ConcurrentMap<K, V> delegate;
    private final Function<K, V> defaultValueFactory;

    private LazyConcurrentMap(ConcurrentMap<K, V> delegate, Function<K, V> defaultValueFactory) {
        this.delegate = delegate;
        this.defaultValueFactory = defaultValueFactory;
    }

    @Override
    public V get(Object key) {
        while (true) {
            V current = delegate.get(key);
            if (current != null)
                return current;

            V defValue = defaultValueFactory.apply((K) key);
            V existing = delegate.putIfAbsent((K) key, defValue);
            if (existing == null) {
                // the new default value did win!
                return defValue;
            }
            return existing; // someone beat us to it
        }
    }

    @Override
    protected ConcurrentMap<K, V> delegate() {
        return delegate;
    }
}
