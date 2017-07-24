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

import static com.github.steveash.synthrec.stat.SamplingTable.createFromMultinomial;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provides smoothing and backoff for conditional distributions:
 * you give it a conditional distribution P(A | B, C) and a back off strategy (e.g. B,C -> C -> {}) and
 * it produces a backoff sampler that first gives you a smoothed P(A | B, C) by applying a scaled marginal P(A | C)
 * that backs off to P(A | C) smoothed by P(A)
 * @author Steve Ash
 */
public class BackoffSmoother<T> {

    private static final double DEFAULT_SMOOTH_ALPHA = 0.05;
    private static final int DEFAULT_MIN_VIRTUAL_COUNT = 500;

    public static class Level {
        final double smoothAlpha;
        final double smoothMinVirtualCount;
        final ImmutableSet<String> parents; // conditioned on vars

        public Level(double smoothAlpha, double smoothMinVirtualCount, Set<String> parents) {
            this.smoothAlpha = smoothAlpha;
            this.smoothMinVirtualCount = smoothMinVirtualCount;
            this.parents = ImmutableSet.copyOf(parents);
        }
    }

    public static class LevelBuilder<T> {
        private String baseName = "unnamed";
        private final List<Level> levels = Lists.newArrayList();
        private double smoothAlpha = DEFAULT_SMOOTH_ALPHA;
        private double smoothMinVirtualCount = DEFAULT_MIN_VIRTUAL_COUNT;

        public LevelBuilder<T> nextOn(String... parents) {
            return nextOn(Arrays.asList(parents));
        }

        public LevelBuilder<T> withSmoothAlpha(double newSmoothAlpha) {
            this.smoothAlpha = newSmoothAlpha;
            return this;
        }

        public LevelBuilder<T> withBaseName(String name) {
            this.baseName = name;
            return this;
        }

        public LevelBuilder<T> withSmoothMinVirtualCount(double newMinVirtualCount) {
            this.smoothMinVirtualCount = newMinVirtualCount;
            return this;
        }

        public LevelBuilder<T> nextOn(Iterable<String> parents) {
            ImmutableSet<String> parentsSet = ImmutableSet.copyOf(parents);
            Preconditions.checkState(parentsSet.size() > 0,
                    "must pass some parents; the unconditional is assumed last"
            );
            levels.add(new Level(this.smoothAlpha, this.smoothMinVirtualCount, parentsSet));
            return this;
        }

        public BackoffSmoother<T> build() {
            return new BackoffSmoother<>(this.baseName, ImmutableList.copyOf(this.levels));
        }
    }

    public static <T> LevelBuilder<T> startingWith(double alpha,
            double minVirtualCount,
            Iterable<String> topLevelParents
    ) {
        return new LevelBuilder<T>()
                .withSmoothAlpha(alpha)
                .withSmoothMinVirtualCount(minVirtualCount)
                .nextOn(topLevelParents);
    }

    public static <T> LevelBuilder<T> startingWith(String... topLevelParents) {
        return startingWith(DEFAULT_SMOOTH_ALPHA, DEFAULT_MIN_VIRTUAL_COUNT, Arrays.asList(topLevelParents));
    }

    public static <T> LevelBuilder<T> startingWith(double alpha, double minVirtualCount, String... topLevelParents) {
        return startingWith(alpha, minVirtualCount, Arrays.asList(topLevelParents));
    }

    private final String name; // for log messages
    private final List<Level> levels;
    private final List<Set<String>> levelParents;   // size is |levels| + 1 (for the unconditional)
    private final List<EmpPriorSmoother> smoothers; // size is |levels|

    // levelDists is the back off dist for each level; for level 0 its just the input dist, level 1 is the backoff for level 0, etc.
    // the last level is the unconditional distribution.  The cardinality is |levelParents|
    List<Map<AssignmentInstance, ? extends Multinomial<T>>> levelDists = Lists.newArrayList(); // size is |levelParents|
    // this is the computed output conditionals. the cardinality is |levelParents - 1| and each contains the smoothed versions
    // of the multinomials (no unconditional because thats already in levelDists)
    List<Map<AssignmentInstance, ? extends Multinomial<T>>> outputs = Lists.newArrayList();

    /**
     * @param levels this should be # of total sampling levels - 1.  So if you want to smooth based on the unconditional
     * marginal then you should have List.size() == 1 with the parents of the top most level and smoothing parameters
     * the uncoditional level is always assumed to be the last level
     */
    private BackoffSmoother(String baseName, List<Level> levels) {
        this.levels = levels;
        this.name = baseName;
        Preconditions.checkArgument(levels.size() > 0, "must pass in at least one level with the parents");
        ArrayList<Set<String>> parents = Lists.newArrayListWithCapacity(levels.size());
        List<EmpPriorSmoother> smoothers = Lists.newArrayListWithCapacity(levels.size());
        for (int i = 0; i < levels.size(); i++) {
            Level level = levels.get(i);
            parents.add(level.parents);
            smoothers.add(new EmpPriorSmoother(level.smoothAlpha, level.smoothMinVirtualCount));
            if (i > 0) {
                Preconditions.checkArgument(parents.get(i - 1).containsAll(parents.get(i)), "levels must be subsets");
            }
        }
        parents.add(ImmutableSet.of()); // the final is alwyas the unconditional
        this.levelParents = parents;
        this.smoothers = smoothers;
    }

    /**
     * Create a conditional sampler that is smoothed by the level description and backs off as described in the
     * level description
     * @param conditional this is the top level conditional to smooth+backoff
     * @return
     */
    public ConditionalSampler<T> smoothSampler(Map<AssignmentInstance, ? extends Multinomial<T>> conditional) {
        smoothFirstPass(conditional);
        ConditionalSampler<T> result = assembleSampler();
        levelDists.clear();
        outputs.clear();
        return result;
    }

    @VisibleForTesting
    void smoothFirstPass(Map<AssignmentInstance, ? extends Multinomial<T>> conditional) {
        initDists(conditional);
        outputs.clear();

        for (int i = 0; i < levels.size(); i++) {
            outputs.add(makeConditional(i));
        }
    }

    private void initDists(Map<AssignmentInstance, ? extends Multinomial<T>> conditional) {
        levelDists.clear();
        levelDists.add(conditional); // first level is the passed in one
        for (int i = 1; i < levelParents.size(); i++) {
            Map<AssignmentInstance, MutableMultinomial<T>> marginal = Marginalizer.marginalizeTo(
                    levelParents.get(i),
                    conditional
            );
            levelDists.add(marginal);
            conditional = marginal;
        }
    }

    private Map<AssignmentInstance, MutableMultinomial<T>> makeConditional(int levelIndex) {
        Map<AssignmentInstance, ? extends Multinomial<T>> dist = levelDists.get(levelIndex);
        Map<AssignmentInstance, MutableMultinomial<T>> results = Maps.newHashMapWithExpectedSize(dist.size());

        for (Entry<AssignmentInstance, ? extends Multinomial<T>> entry : dist.entrySet()) {
            AssignmentInstance assign = entry.getKey();
            Multinomial<T> backoff = lookupBackoff(levelIndex + 1, assign);
            EmpPriorSmoother smoother = smoothers.get(levelIndex);
            MutableMultinomial<T> priorCopy = backoff.copy();
            smoother.smoothPriorCopy(entry.getValue(), priorCopy, name + "(" + assign.toKeyValueString() + ")");
            results.put(assign, priorCopy);
        }
        return results;
    }

    private Multinomial<T> lookupBackoff(int startAtIndex, AssignmentInstance instance) {
        for (int i = startAtIndex; i < levelParents.size(); i++) {
            instance = instance.subset(levelParents.get(i));
            Map<AssignmentInstance, ? extends Multinomial<T>> dist = levelDists.get(i);
            Multinomial<T> maybe = dist.get(instance);
            if (maybe != null) {
                return maybe;
            }
        }
        throw new IllegalStateException("shouldve hit the unconditional dist");
    }

    private ConditionalSampler<T> assembleSampler() {
        Preconditions.checkState(outputs.size() == levels.size(), "missing some levels");
        Map<AssignmentInstance, ? extends Multinomial<T>> lastDist = levelDists.get(levelParents.size() - 1);
        Preconditions.checkState(lastDist.size() == 1, "should end on unconditional");
        Multinomial<T> unconditional = checkNotNull(lastDist.get(AssignmentInstance.EMPTY_ASSIGNMENT));

        ConditionalSampler<T> last = ConditionalSampler.adaptSampler(createFromMultinomial(unconditional));
        for (int i = outputs.size() - 1; i >= 0; i--) {
            Set<String> parents = levelParents.get(i);
            Map<AssignmentInstance, SamplingTable<T>> conditionalSampler =
                    SamplingTable.createConditionalFromMultinomial(outputs.get(i));
            BackoffSampler<T> sampler = new BackoffSampler<>(conditionalSampler, last, parents);
            last = sampler;
        }
        return last;
    }
}
