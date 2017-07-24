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

import java.util.HashMap;
import java.util.Map;

import javax.swing.text.AbstractDocument.ElementEdit;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Helper utilities for distributions (like Multinomials and conditional multinomials which i wish that
 * wouldve represented as a real type)
 * @author Steve Ash
 */
public class Dists {

    public static class ConditionalBuilder<T> {
        private Map<AssignmentInstance, MutableMultinomial<T>> conds = Maps.newHashMap();

        public MutableMultinomial<T> conditionalOn(String k0, Object v0, Object... rest) {
            AssignmentInstance inst = assignFor(k0, v0, rest);
            return multinomialFor(inst);
        }

        public MutableMultinomial<T> multinomialFor(AssignmentInstance inst) {
            return conds.computeIfAbsent(inst, (ai) -> MutableMultinomial.createUnknownMax());
        }

        public AssignmentInstance assignFor(String k0, Object v0, Object... rest) {
            HashMap<String, Object> assign = Maps.newHashMap();
            assign.put(k0, v0);
            Preconditions.checkArgument(rest.length % 2 == 0, "must pass key, value pairs as rest");
            for (int i = 0; i < rest.length; i += 2) {
                assign.put((String)rest[i], rest[i+1]);
            }
            return AssignmentInstance.make(assign);
        }

        public Adder adderFor(String k0, Object v0, Object... rest) {
            return new Adder(conditionalOn(k0, v0, rest));
        }

        public Adder adderFor(AssignmentInstance inst) {
            return new Adder(multinomialFor(inst));
        }

        public Map<AssignmentInstance, MutableMultinomial<T>> getConditional() {
            return conds;
        }

        public class Adder {
            private final MutableMultinomial<T> target;

            Adder(MutableMultinomial<T> target) {this.target = target;}

            public Adder add(T element, double count) {
                target.add(element, count);
                return this;
            }

            public Adder increment(T element) {
                target.add(element, 1.0);
                return this;
            }

            public Adder incrementMany(T... elements) {
                for (T element : elements) {
                    increment(element);
                }
                return this;
            }
        }
    }

    public static <T> ConditionalBuilder<T> condBuilder() {
        return new ConditionalBuilder<>();
    }
}
