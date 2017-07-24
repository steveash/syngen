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
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * A gen assignment that delegates to another
 * @author Steve Ash
 */
public class OverridingGenAssignment implements GenAssignment {

    public static MutableGenAssignment flatten(GenAssignment assignment) {
        Map<String, Object> sink = Maps.newHashMap();
        assignment.visitAll(sink::putIfAbsent);
        return new MutableGenAssignment(sink);
    }

    /**
     * Take this genAssignment, collapse it, and add it (if missing) into the given sink assignment
     * @param assignment
     * @param sink
     */
    public static void flattenTo(GenAssignment assignment, MutableGenAssignment sink) {
        assignment.visitAll(sink::putIfAbsent);
    }

    private Map<String, Object> values = Maps.newHashMap();
    private final GenAssignment delegate;

    public OverridingGenAssignment(GenAssignment delegate) {this.delegate = delegate;}

    @Override
    public AssignmentInstance subset(Set<String> keysToGet) {
        if (keysToGet.isEmpty()) {
            return AssignmentInstance.EMPTY_ASSIGNMENT;
        }
        Map<String, Object> subset = Maps.newHashMapWithExpectedSize(keysToGet.size());
        for (String key : keysToGet) {
            subset.put(key, get(key));
        }
        return AssignmentInstance.make(subset);
    }

    @Override
    public void put(String key, Object value) {
        Object prevValue = values.put(key, value);
        Preconditions.checkState(prevValue == null, "cant put the same value into the same instance twice", key);
    }

    @Override
    public Object get(String key) {
        return checkNotNull(tryGet(key), "no value for key ", key);
    }

    @Override
    @Nullable
    public Object tryGet(String key) {
        Object maybe = values.get(key);
        if (maybe == null) {
            return delegate.tryGet(key);
        }
        return maybe;
    }

    @Override
    public void visitAll(AssignmentVisitor visitor) {
        for (Entry<String, Object> entry : values.entrySet()) {
            visitor.onEntry(entry.getKey(), entry.getValue());
        }
        delegate.visitAll(visitor);
    }

    @Override
    public String toString() {
        return "GenAssignment{" +
                "values=" + values +
                '}';
    }
}
