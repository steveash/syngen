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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.MissingPolicy;
import com.github.steveash.synthrec.domain.MultivalueIterable;
import com.github.steveash.synthrec.stat.JointDensityIterator;
import com.github.steveash.synthrec.stat.Multinomial;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;

/**
 * An (immutable) assignment of a particular set of categorical random variables; This can be a fully
 * observed/evidence assignment where you fully set the random variable to a particular value OR if you have
 * unobserved values (i.e. uncertainty) then you can set a variable to a particular distribution
 * <p>
 * That is one assignment might be: race -> white, fname -> steve, lname -> ash (every variable is observed)
 * Or if you can't observe race, you can also capture the uncertainty in the race distribution by creating assignment of:
 * race -> [white: 90%, hispanic: 10%], name -> steve, lname -> ash
 * <p>
 * Since these are all required to be categorical with total # of instantiations known ahead of time we can
 * enmerate all instantiations of the assignment even when it just puts a distribution over any subset of the random
 * variables; obviously adding more distributions instead of clamped values grows the number of instantiations
 * multiplicatively so use wisely
 * <p>
 * An assignment is then enumerated which returns a count assignment estimate -- which is an instance (particular
 * fixed assignment no uncertainty) + the certainty values (point estimate) of that particular assignment instance
 * @author Steve Ash
 */
public class CountAssignment {

    public static CountAssignment fromObserved(Map<String,Object> observedValues) {
        return new CountAssignment(ImmutableMap.copyOf(observedValues), null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String,Object> observed = Maps.newHashMap();
        private Map<String, Multinomial<?>> unobserved = Maps.newHashMap();

        public Builder putObserved(String key, @Nullable Object value) {
            if (value != null) {
                observed.put(key, value);
            }
            return this;
        }

        public Builder putUnobserved(String key, @Nullable Multinomial<?> normalizedDensity) {
            if (normalizedDensity != null && !normalizedDensity.entries().isEmpty()) {
                unobserved.put(key, normalizedDensity);
            }
            return this;
        }

        public CountAssignment build() {
            Preconditions.checkArgument(Sets.intersection(observed.keySet(), unobserved.keySet()).isEmpty(),
                    "cant set same key twice");
            return new CountAssignment(ImmutableMap.copyOf(observed), ImmutableMap.copyOf(unobserved));
        }
    }

    private final ImmutableMap<String, Object> observedVars;
    private final ImmutableMap<String, Multinomial<?>> unobservedVars;

    // the unobserved densities should _already_ be normalized
    private CountAssignment(ImmutableMap<String, Object> observedVars,
            ImmutableMap<String, Multinomial<?>> unobservedVars
    ) {
        this.observedVars = observedVars;
        if (unobservedVars != null && !unobservedVars.isEmpty()) {
            this.unobservedVars = unobservedVars;
        } else {
            this.unobservedVars = null;
        }
    }

    public boolean isFullyObserved() {
        return unobservedVars == null;
    }

    /**
     * Returns the observed value (if observed) or returns the density for this factor if its an unobserved
     * value)
     * @param factor
     * @return
     */
    @Nullable
    public Object valueFor(String factor) {
        Object obsVal = observedVars.get(factor);
        if (obsVal == null) {
            if (unobservedVars != null) {
                return unobservedVars.get(factor);
            }
        }
        return obsVal;
    }

    /**
     * Returns back all of the estimates for this subset of the variables from this assignment. Note that depending on
     * the missing policy this might be an empty result; if this assignment is missing a value and you skip the whole
     * record then you will get back an iterable of size zero
     * @param onlyTheseVarKeys
     * @param missingPolicy
     * @return
     */
    public Iterable<CountAssignmentEstimate> enumerateSubset(Set<String> onlyTheseVarKeys,
            MissingPolicy missingPolicy
    ) {
        if (onlyTheseVarKeys.isEmpty()) {
            return ImmutableList.of();
        }
        if (isFullyObserved()) {
            return subsetObserved(onlyTheseVarKeys, missingPolicy);
        }
        // are any of the asked for vars unobserved?
        SetView<String> unobservedRequested = Sets.intersection(onlyTheseVarKeys, unobservedVars.keySet());
        if (unobservedRequested.isEmpty()) {
            // regardless if this is true due to missing or due to it really only being observed things; it can be
            // handled by the observed only case
            return subsetObserved(onlyTheseVarKeys, missingPolicy);
        }
        // we have some observed and some unobserved; first lets get the observed out of the way (and if we're
        // missing some we might alrady have an out)
        Iterable<String> mustHaveObserved = Sets.difference(onlyTheseVarKeys, unobservedVars.keySet());
        // note that we might be missing an unobserved var that we need -- but in this case its ok we will get
        // back a null and react appropriately
        ImmutableMap<String, Object> maybeObserved = collectObserved(mustHaveObserved, missingPolicy);
        if (maybeObserved == null) {
            return ImmutableList.of();
        }
        ArrayList<String> unobNames = Lists.newArrayList();
        ArrayList<Multinomial<?>> unobDens = Lists.newArrayList();
        for (String name : unobservedRequested) {
            unobNames.add(name);
            unobDens.add(checkNotNull(unobservedVars.get(name)));
        }
        return FluentIterable.from(MultivalueIterable.enumerate(maybeObserved))
                .transformAndConcat( map -> () -> {
                    JointDensityIterator iter = new JointDensityIterator(unobDens);
                    return Iterators.transform(iter, e -> {
                        Object2DoubleArrayMap<String> uncertains = new Object2DoubleArrayMap<>(e.entries().length);
                        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                        builder.putAll(map);
                        for (int i = 0; i < unobNames.size(); i++) {
                            Object value = e.entry(i);
                            String name = unobNames.get(i);
                            double prob = e.probability(i);
                            builder.put(name, value);

                            Preconditions.checkState(prob > 0.0, "shouldn't happen");
                            if (prob < 1.0) {
                                uncertains.put(name, prob);
                            }
                        }
                        return makeEstimate(builder.build(), uncertains);
                    });
                });
    }

    private Iterable<CountAssignmentEstimate> subsetObserved(Iterable<String> onlyTheseKeys,
            MissingPolicy missingPolicy
    ) {
        ImmutableMap<String, Object> observed = collectObserved(onlyTheseKeys, missingPolicy);
        if (observed == null) {
            return ImmutableList.of();
        }
        return FluentIterable.from(MultivalueIterable.enumerate(observed))
                .transform(map -> makeEstimate(map, null));
    }

    @Nullable // if cant collect due to missing policy
    private ImmutableMap<String, Object> collectObserved(Iterable<String> onlyTheseKeys, MissingPolicy missingPolicy) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        for (String key : onlyTheseKeys) {
            Object value = this.observedVars.get(key);
            if (value == null || Constants.MISSING.equals(value)) {
                if (missingPolicy == MissingPolicy.SKIP_WHOLE_RECORD) {
                    return null;
                } else if (missingPolicy == MissingPolicy.PLACEHOLDER) {
                    value = Constants.MISSING;
                } else {
                    throw new IllegalStateException("unknown policy");
                }
            }
            builder.put(key, value);
        }
        return builder.build();
    }

    private CountAssignmentEstimate makeEstimate(Map<String, Object> instance,
            Object2DoubleArrayMap<String> nonOneEntries
    ) {
        return new CountAssignmentEstimate(AssignmentInstance.make(instance), nonOneEntries);
    }


}
