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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.util.MoreCollections;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * One particular -- fixed -- instance of a particular assignment of categorical random variables.  If you have any
 * uncertainty then you construct the appropriate CountAssignment with those assignments and enumerate the instances
 * @author Steve Ash
 */
public class AssignmentInstance implements AssignmentProvider {

    public static final AssignmentInstance EMPTY_ASSIGNMENT = new AssignmentInstance(ImmutableMap.of());
    private static final MapJoiner MAP_JOINER = Joiner.on(',').withKeyValueSeparator('=');

    public static AssignmentInstance make(String key1, Object val1) { return make(key1, val1, null, null); }

    public static AssignmentInstance make(String key1, Object val1, String key2, Object val2) {
        return make(key1,
                val1,
                key2,
                val2,
                null,
                null
        );
    }

    public static AssignmentInstance make(String key1,
            Object val1,
            String key2,
            Object val2,
            String key3,
            Object val3
    ) {
        Builder<String, Object> builder = ImmutableMap.builder();
        if (key1 != null) {
            builder.put(key1, val1);
        }
        if (key2 != null) {
            builder.put(key2, val2);
        }
        if (key3 != null) {
            builder.put(key3, val3);
        }
        return AssignmentInstance.make(builder.build());
    }

    public static AssignmentInstance make(Map<String,Object> assign) {
        if (assign.isEmpty()) {
            return EMPTY_ASSIGNMENT;
        }
        return new AssignmentInstance(assign);
    }

    private final int cachedHash;
    private final ImmutableMap<String, Object> assign;

    private AssignmentInstance(Map<String, Object> assign) {
        this.assign = ImmutableMap.copyOf(assign);
        this.cachedHash = this.assign.hashCode();
    }

    public AssignmentInstance subset(Set<String> keysToSelect) {
        if (assign.keySet().equals(keysToSelect)) {
            return this;
        }
        ImmutableMap<String, Object> build = MoreCollections.subsetMap(assign, keysToSelect);

        return AssignmentInstance.make(build);
    }

    public AssignmentInstance difference(Set<String> keysToSubtract) {
        if (keysToSubtract.isEmpty()) {
            return this;
        }
        Builder<String, Object> builder = ImmutableMap.builder();
        for (Entry<String, Object> entry : assign.entrySet()) {
            if (!keysToSubtract.contains(entry.getKey())) {
                builder.put(entry);
            }
        }
        return AssignmentInstance.make(builder.build());
    }

    public boolean containsMissing() {
        return assign.values().contains(Constants.MISSING);
    }

    public ImmutableMap<String, Object> getAssignment() {
        return assign;
    }

    public Object get(String key, Object defaultValue) {
        return assign.getOrDefault(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        return (String) get(key, defaultValue);
    }

    public int size() {
        return assign.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssignmentInstance that = (AssignmentInstance) o;

        if (cachedHash != that.cachedHash) return false;
        return assign.equals(that.assign);
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    @Override
    public String toString() {
        return "{" + assign + '}';
    }

    public String toKeyValueString() {
        return MAP_JOINER.join(assign);
    }
}
