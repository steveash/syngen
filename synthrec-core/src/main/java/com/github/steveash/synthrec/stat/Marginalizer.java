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

package com.github.steveash.synthrec.stat;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.steveash.synthrec.collect.LazyMap;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * marginalizes a joint or conditional distribution represented as a Map<AssignmentInstance,Multinomial>
 * @author Steve Ash
 */
public class Marginalizer {

    public static <T> MutableMultinomial<T> marginalize(Map<AssignmentInstance,? extends Multinomial<T>> source) {
        Map<AssignmentInstance, MutableMultinomial<T>> resultMap = marginalizeTo(
                ImmutableSet.of(),
                source
        );
        if (resultMap.isEmpty()) {
            return MutableMultinomial.createUnknownMax();
        }
        Preconditions.checkState(resultMap.size() == 1);
        return Iterables.getOnlyElement(resultMap.values());
    }

    public static <T> Map<AssignmentInstance,MutableMultinomial<T>> marginalizeTo(Set<String> newParents,
            Map<AssignmentInstance,? extends Multinomial<T>> source) {

        LazyMap<AssignmentInstance, MutableMultinomial<T>> result = LazyMap.makeSupply( MutableMultinomial::createUnknownMax);
        for (Entry<AssignmentInstance, ? extends Multinomial<T>> entry : source.entrySet()) {
            AssignmentInstance subset = entry.getKey().subset(newParents);
            result.get(subset).addMultinomial(entry.getValue());
        }
        // dont return the lazy map because it will create empty multinomials when it has a key miss!
        return result.delegate();
    }
}
