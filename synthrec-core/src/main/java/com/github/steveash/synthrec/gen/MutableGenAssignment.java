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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.util.MoreCollections;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Container of generated values that is passed through the DAG and accumulates sampled values
 * @author Steve Ash
 */
public class MutableGenAssignment implements GenAssignment {

    private final Map<String, Object> values;

    public MutableGenAssignment() {
        values = Maps.newHashMap();
    }

    public MutableGenAssignment(Map<String,Object> useThisMap) {
        this.values = useThisMap;
    }

    @Override
    public AssignmentInstance subset(Set<String> keysToGet) {
        return AssignmentInstance.make(MoreCollections.subsetMap(values, keysToGet));
    }

    @Override
    public void put(String key, Object value) {
        Object prevValue = values.put(key, value);
        Preconditions.checkState(prevValue == null, "cant put the same value into the same instance twice", key);
    }

    public void putIfAbsent(String key, Object value) {
        values.putIfAbsent(key, value);
    }

    @Override
    public Object get(String key) {
        return checkNotNull(tryGet(key), "no value for key ", key);
    }

    @Override
    @Nullable
    public Object tryGet(String key) {
        return values.get(key);
    }

    public int size() {
        return values.size();
    }

    public Map<String, Object> getValues() {
        return values;
    }

    @Override
    public void visitAll(AssignmentVisitor visitor) {
        for (Entry<String, Object> entry : values.entrySet()) {
            visitor.onEntry(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String toString() {
        return "GenAssignment{" +
                "values=" + values +
                '}';
    }
}
