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

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * A factor in a DAG of factors that needs to be counted; note that we don't know anything about parents here
 * @author Steve Ash
 */
public class CountFactor implements Serializable {

    private static final long serialVersionUID = -7439732610702053400L;

    private final String name;
    final MutableMultinomial<DehydratedAssignment> unconditional = new MutableMultinomial<>(-1);
    final Map<DehydratedAssignment, MutableMultinomial<DehydratedAssignment>> conditional = Maps.newHashMap();

    public CountFactor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void increment(DehydratedAssignment instance, DehydratedAssignment parentOrNull) {
        add(instance, parentOrNull, 1.0);
    }

    public void add(DehydratedAssignment instance,
            DehydratedAssignment parentOrNull,
            double amountToAdd
    ) {
        if (parentOrNull == null) {
            unconditional.add(instance, amountToAdd);
        } else {
            MutableMultinomial<DehydratedAssignment> maybe = conditional.get(parentOrNull);
            if (maybe == null) {
                maybe = new MutableMultinomial<>(-1);
                conditional.put(parentOrNull, maybe);
            }
            maybe.add(instance, amountToAdd);
        }
    }

    public MutableMultinomial<DehydratedAssignment> makeJoint() {
        if (conditional.isEmpty()) {
            return unconditional;
        }
        MutableMultinomial<DehydratedAssignment> joint = new MutableMultinomial<>(-1);
        for (Entry<DehydratedAssignment, MutableMultinomial<DehydratedAssignment>> entry : conditional.entrySet()) {
            for (Object2DoubleMap.Entry<DehydratedAssignment> entry2 : entry.getValue().entries()) {
                joint.add(DehydratedAssignment.merge(entry.getKey(), entry2.getKey()), entry2.getDoubleValue());
            }
        }
        return joint;
    }
}
