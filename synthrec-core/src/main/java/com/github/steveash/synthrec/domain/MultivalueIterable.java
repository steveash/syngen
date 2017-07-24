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

package com.github.steveash.synthrec.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.steveash.synthrec.collect.SlotIndexIterator;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * @author Steve Ash
 */
public class MultivalueIterable {

    /**
     * Takes a map<string,object> that might have one or more entry that is a multivalue value and
     * returns the expansion of that -- iterating through all unique combinations of the expanded values
     *
     * @param maybeHasMulti
     * @return
     */
    public static Iterable<Map<String,Object>> enumerate(Map<String,Object> maybeHasMulti) {
        int count = countMulti(maybeHasMulti);
        if (count == 0) {
            // no multi values happy path of just returning itself
            return ImmutableList.of(maybeHasMulti);
        }
        if (count == 1) {
            return overSingle(maybeHasMulti);
        }
        return overMulti(maybeHasMulti, count);
    }

    private static Iterable<Map<String, Object>> overMulti(Map<String, Object> map, int count) {
        final Map<String, Object> fixed = Maps.newHashMapWithExpectedSize(map.size() - count);
        List<String> multiNames = Lists.newArrayListWithCapacity(count);
        List<List<Object>> multis = Lists.newArrayListWithCapacity(count);
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Multivalue) {
                multiNames.add(key);
                multis.add(((Multivalue) value).getValueBag());
            } else {
                fixed.put(key, value);
            }
        }
        final int outputSize = map.size();
        return () -> {
            Iterator<IntArrayList> iter = SlotIndexIterator.makeFor(multis);
            return new AbstractIterator<Map<String, Object>>() {
                @Override
                protected Map<String, Object> computeNext() {
                    if (!iter.hasNext()) {
                        return endOfData();
                    }
                    IntArrayList indexes = iter.next();
                    HashMap<String, Object> output = Maps.newHashMapWithExpectedSize(outputSize);
                    output.putAll(fixed);
                    for (int i = 0; i < multiNames.size(); i++) {
                        List<Object> objs = multis.get(i);
                        output.put(multiNames.get(i), objs.get(indexes.getInt(i)));
                    }
                    return output;
                }
            };
        };
    }

    private static Iterable<Map<String, Object>> overSingle(Map<String, Object> map) {
        final Map<String, Object> fixed = Maps.newHashMapWithExpectedSize(map.size());
        String multiNameTmp = null;
        Multivalue<?> multi = null;
        for (Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Multivalue) {
                multi = (Multivalue<?>) entry.getValue();
                multiNameTmp = entry.getKey();
            } else {
                fixed.put(entry.getKey(), entry.getValue());
            }
        }
        if (multi.size() == 1) {
            // just the one so fast path it
            fixed.put(multiNameTmp, multi.getValueBag().get(0));
            return ImmutableList.of(fixed);
        }
        // there are multiple to iterate over
        final String multiName = multiNameTmp;
        final ImmutableList<?> bag = multi.getValueBag();
        int outputSize = fixed.size() + 1;
        return () -> {
            UnmodifiableIterator<?> iter = bag.iterator();
            return new AbstractIterator<Map<String, Object>>() {
                @Override
                protected Map<String, Object> computeNext() {
                    if (!iter.hasNext()) {
                        return endOfData();
                    }
                    Object next = iter.next();
                    HashMap<String, Object> output = Maps.newHashMapWithExpectedSize(outputSize);
                    output.putAll(fixed);
                    output.put(multiName, next);
                    return output;
                }
            };
        };
    }

    private static int countMulti(Map<String, Object> maybe) {
        int count = 0;
        for (Object val : maybe.values()) {
            if (val instanceof Multivalue) {
                count += 1;
            }
        }
        return count;
    }
}
