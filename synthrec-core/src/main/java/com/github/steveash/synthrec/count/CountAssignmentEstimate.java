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

package com.github.steveash.synthrec.count;

import java.util.List;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;

/**
 * An estimate is one instance (i.e. fixed values for a set of random vars) + the certainty values over that instance;
 * if every one of the variables in the instance is fully observed thne the certainty values are always 1.0
 * @author Steve Ash
 */
public class CountAssignmentEstimate {

    // the actual assignment itself; this assignment will have no distributions; this is a fully observed/clamped point estimate
    private final AssignmentInstance assignment;
    // if any of the assignments were distributions then we need to track the probabilty of this assignment; for most
    // of these they are treated as observed evidence values and thus their probability is 1.0; for the unobserved
    // then we record an entry here with the value.  Thus for an assignment that already was fully observed; this
    // map will be empty (and in fact we just store a null in the field)
    // the key is the name of the random variable (i.e. the key in the assignment)
    private final Object2DoubleArrayMap<String> variableCertainty;

    CountAssignmentEstimate(AssignmentInstance assignment, Object2DoubleArrayMap<String> variableCertainty) {
        this.assignment = assignment;
        this.variableCertainty = variableCertainty;
        if (variableCertainty != null) {
            Preconditions.checkArgument(variableCertainty.defaultReturnValue() <= 0);
            // also all entries should be > 0
        }
    }

    public AssignmentInstance getAssignment() {
        return assignment;
    }

    public ImmutableMap<String,Object> getAssignmentMap() {
        return assignment.getAssignment();
    }

    public int size() {
        return assignment.size();
    }

    public boolean isFullyObserved() {
        return variableCertainty == null;
    }

    public double certaintyOfVariable(String varName) {
        if (variableCertainty == null) {
            return 1.0;
        }
        double maybe = variableCertainty.getDouble(varName);
        // if this is missing then its a clamped variable value; not uncertain
        if (maybe <= 0) {
            return 1.0;
        }
        return maybe; // this is a partial certainty unobserved value
    }

    @Override
    public String toString() {
        return "CountAssignmentEstimate{" +
                "assignment=" + assignment +
                ", variableCertainty=" + variableCertainty +
                '}';
    }

    public String toEntryString() {
            StringBuilder sb = new StringBuilder();
            boolean isFirst = true;
        ImmutableMap<String, Object> map = this.assignment.getAssignment();
        List<String> keys = Ordering.natural().sortedCopy(map.keySet());
            for (String key : keys) {
                if (!isFirst) sb.append(",");
                isFirst = false;
                sb.append(key).append("=").append(map.get(key));
            }
            if (variableCertainty != null && !variableCertainty.isEmpty()) {
                sb.append(",**");
            }
            return sb.toString();
        }
}
