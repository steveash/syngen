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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.github.steveash.synthrec.collect.VocabCounter;
import com.github.steveash.synthrec.collect.Vocabulary;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.MissingPolicy;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.util.PrintUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

/**
 * A directed graph of random variables that is used to count transitions in a hierarchical model efficiently
 * Also records some simple stats and records an independent distribution (ignoring parents) for each factor as
 * well.  This independent distribution can be used to inform the dependent distribution later to smooth it (like a prior)
 *
 * TODO split this into a builder and a buildee
 * @author Steve Ash
 */
public class CountDag implements Serializable {

    private static final long serialVersionUID = -3931635880943282125L;

    // distrib -> vocab registry for all distributions
    private DistribVocabRegistry vocabRegistry = new DistribVocabRegistry();
    private transient VocabHydrater vocabHydrater = new VocabHydrater(vocabRegistry);
    // factors for each variable
    private Map<String, CountFactor> factorNameMap = Maps.newHashMap();
    // list of the groups that we count
    private Map<String,FactorGroup> factorGroups = Maps.newHashMap();
    // key is child, parent is list of factor names for the parent
    private ArrayListMultimap<String, String> parents = ArrayListMultimap.create();
    // stats for each factor (keyset of this matches keyset of factorNameMap); this has stats plus vocab counters
    private Map<String, FactorStats> factorStatsMap = Maps.newHashMap();
    // sensitive info for later
    private Set<String> sensitiveFactorNames = Sets.newHashSet();
    private Set<Pair<String, String>> sensitiveFactorSubFieldNames = Sets.newHashSet();

    // the set of field names that you want to run reducers on
    private Set<String> reduceFactors = Sets.newHashSet();

    // after its constructed we build some dag structures so that we count efficiently
    private boolean isFrozen = false;

    private MissingPolicy missingPolicy = MissingPolicy.SKIP_WHOLE_RECORD;

    public CountFactor newFactor(String name) {
        throwIfFrozen();
        CountFactor cf = new CountFactor(name);
        CountFactor prev = factorNameMap.put(name, cf);
        if (prev != null) {
            throw new IllegalArgumentException("already have a factor named " + name);
        }
        factorStatsMap.put(name, new FactorStats(name, vocabRegistry));
        return cf;
    }

    public CountFactor getFactor(String name) {
        return checkNotNull(factorNameMap.get(name), "asking for a factor that doesn't exist ", name);
    }

    public FactorGroup getFactorGroup(String name) {
        return checkNotNull(factorGroups.get(name), "asking for a factor group that doesn't exist", name);
    }

    public FactorStats getFactorStats(String name) {
        return checkNotNull(factorStatsMap.get(name), "asking for a factor stats that doesnt exist", name);
    }

    public CountFactor newFactorWithParents(String name, String... parents) {
        throwIfFrozen();
        CountFactor child = newFactor(name);
        List<String> parentList = Arrays.asList(parents);
        this.parents.putAll(name, parentList);

        return child;
    }

    /**
     * Indicates that you want to count a particular factor as conditionally indepedent of other _non parent_ factors
     * you can still have parents and you will count that conditional distribution
     * @param factor
     * @see #countJoint(String, CountFactor...)
     */
    public void countIndependant(CountFactor factor) {
        List<String> parents = this.parents.get(factor.getName());
        HashSet<String> assignKeys = Sets.newHashSetWithExpectedSize(parents.size() + 1);
        assignKeys.addAll(parents);
        assignKeys.add(factor.getName());
        FactorGroup group = new FactorGroup(factor.getName(), factor, parents,
                ImmutableList.of(factor.getName()), assignKeys
        );
        factorGroups.put(factor.getName(), group);
    }

    /**
     * Indicates that you want to count the joint distribution of a set of factors; note that this joint clique that you
     * will be counting is still conditioned on the set of all parents
     * @param cliqueName
     * @param group
     * @return
     */
    public CountFactor countJoint(String cliqueName, CountFactor... group) {
        Preconditions.checkArgument(group.length > 0);
        if (group.length == 1) {
            countIndependant(group[0]);
            return null;
        }
        LinkedHashSet<String> allParents = Sets.newLinkedHashSet();
        LinkedHashSet<String> assignKeys = Sets.newLinkedHashSet();
        for (CountFactor factor : group) {
            List<String> thisParents = parents.get(factor.getName());
            allParents.addAll(thisParents);
            assignKeys.add(factor.getName());
        }
        ImmutableList<String> nonParents = ImmutableList.copyOf(assignKeys);
        assignKeys.addAll(allParents);
        CountFactor groupFactor = new CountFactor(cliqueName);
        FactorGroup newGroup = new FactorGroup(cliqueName, groupFactor, ImmutableList.copyOf(allParents),
                nonParents, assignKeys
        );
        factorGroups.put(cliqueName, newGroup);
        return groupFactor;
    }

    /**
     * Marks an entire field as sensitive; if this factor emits subfields then all subfields are sensitive
     * @param factor
     */
    public void markFactorSensitive(CountFactor factor) {
        throwIfFrozen();
        sensitiveFactorNames.add(factor.getName());
    }

    public void markFactorSubFieldSensitive(CountFactor factor, String subField) {
        throwIfFrozen();
        sensitiveFactorSubFieldNames.add(Pair.of(factor.getName(), subField));
    }

    public void markFactorReduced(CountFactor factor) {
        throwIfFrozen();
        reduceFactors.add(factor.getName());
    }

    public void freeze() {
        throwIfFrozen();
        validateCounting();
        isFrozen = true;
    }

    public synchronized List<FactorGroup> topologicalOrdering() {
        throwIfNotFrozen();
        List<FactorGroup> cliques = makeCliqueGroups();
        MutableGraph<Integer> graph = buildCliqueGraph(cliques);

        LinkedList<FactorGroup> result = Lists.newLinkedList();
        IntOpenHashSet tempMark = new IntOpenHashSet();
        IntOpenHashSet mark = new IntOpenHashSet();
        for (int i = 0; i < cliques.size(); i++) {
            if (mark.contains(i)) {
                continue; // already marked
            }
            visit(i, graph, tempMark, mark, result, cliques);
        }
        return result;
    }

    // some of the factorGroups are entirely contained in other factor groups (like when
    // you count the joint of something). We need a list of the cliques which should not
    // include duplicates
    private List<FactorGroup> makeCliqueGroups() {
        List<FactorGroup> result = Lists.newArrayListWithCapacity(factorGroups.size());
        Set<String> factorNames = Sets.newHashSet();
        for (FactorGroup group : factorGroups.values()) {
            if (isContained(group)) {
                continue;
            }
            for (String factorName : group.factorNonParentsName) {
                if (!factorNames.add(factorName)) {
                    throw new IllegalArgumentException("have two factor groups with the same factor without being in " +
                            "the same clique " + group + " already added " + factorNames);
                }
            }
            result.add(group);
        }

        return result;
    }

    private boolean isContained(FactorGroup group) {
        for (FactorGroup candidate : factorGroups.values()) {
            if (group == candidate) continue; // dont check against yourself
            if (candidate.factorNonParentsName.containsAll(group.factorNonParentsName)) {
                return true;
            }
        }
        return false;
    }

    private static MutableGraph<Integer> buildCliqueGraph(List<FactorGroup> cliques) {
        MutableGraph<Integer> graph = GraphBuilder.<Integer>directed()
                .allowsSelfLoops(false)
                .expectedNodeCount(cliques.size())
                .build();
        // we want a graph of the groups
        for (int i = 0; i < cliques.size(); i++) {
            FactorGroup group = cliques.get(i);
            graph.addNode(i);
            for (String parent : group.getFactorParentsName()) {
                // find the parent group -- we're going to just agglom all of these...
                for (int j = 0; j < cliques.size(); j++) {
                    if (i == j) continue;
                    FactorGroup maybeParent = cliques.get(j);
                    if (maybeParent.factorNonParentsName.contains(parent)) {
                        graph.putEdge(j, i);
                    }
                }
            }
        }
        return graph;
    }

    private static void visit(int node,
            MutableGraph<Integer> graph,
            IntOpenHashSet tempMark,
            IntOpenHashSet mark,
            LinkedList<FactorGroup> result,
            List<FactorGroup> cliques
    ) {
        Preconditions.checkState(!tempMark.contains(node), "cycle detected");
        if (mark.contains(node)) return;
        tempMark.add(node);
        for (Integer child : graph.successors(node)) {
            visit(child, graph, tempMark, mark, result, cliques);
        }
        mark.add(node);
        tempMark.remove(node);
        result.addFirst(cliques.get(node));
    }

    private AssignmentInstance hydrate(DehydratedAssignment assign) {
        return vocabHydrater.hydrate(assign);
    }

    @VisibleForTesting
    synchronized DehydratedAssignment dehydrate(AssignmentInstance instance) {
        return vocabHydrater.dehydrate(instance);
    }

    public Stream<SensitiveDistrib> allSensitiveDistribs() {
        return Stream.concat(sensitiveFields(), sensitiveSubFields());
    }

    public Set<String> getReduceFactors() {
        return reduceFactors;
    }

    private Stream<SensitiveDistrib> sensitiveSubFields() {
        return factorNameMap.keySet().stream()
                .flatMap(key -> {
                    FactorStats stats = factorStatsMap.get(key);
                    if (!stats.isSketches()) {
                        return Stream.empty();
                    }
                    return stats.getSubFieldNames().stream()
                            .filter(subf ->
                                    sensitiveFactorNames.contains(key) || sensitiveFactorSubFieldNames.contains(Pair.of(
                                            key,
                                            subf
                                    ))
                            ).map(subf -> new SensitiveDistrib(
                                    key,
                                    subf,
                                    vocabRegistry.resolveSubFieldVocabFor(key, subf),
                                    stats.getSubFieldVocab(subf)
                            ));
                });
    }

    private Stream<SensitiveDistrib> sensitiveFields() {
        return factorNameMap.keySet().stream()
                .filter(sensitiveFactorNames::contains)
                .filter(key -> !this.factorStatsMap.get(key).isSketches())
                .map(key -> new SensitiveDistrib(
                                key,
                                null,
                                vocabRegistry.resolveVocabForDistrib(key),
                                this.factorStatsMap.get(key).getValueVocab()
                        )
                );
    }

    private void validateCounting() {
        // make sure that all factors declared were put into some factor group
        Set<String> remainingNames = Sets.newHashSet(factorNames());
        for (FactorGroup group : factorGroups.values()) {
            group.factorNonParentsName.forEach(remainingNames::remove);
        }
        if (!remainingNames.isEmpty()) {
            throw new IllegalStateException("You declared some factors that aren't in any factor group for counting. " +
                    "that means that you probably decalred the factor and forgot to indicate how to count it with " +
                    "countIndependent() or countJoint()");
        }
    }

    public Set<String> factorNames() {
        return factorNameMap.keySet();
    }

    public synchronized CountAssignment add(CountAssignment assignment) {
        throwIfNotFrozen();
        updateStats(assignment);
        for (FactorGroup factorGroup : factorGroups.values()) {
            Set<String> subsetKeys = factorGroup.getAssignmentKeys();
            for (CountAssignmentEstimate cae : assignment.enumerateSubset(subsetKeys, missingPolicy)) {
                Preconditions.checkState(cae.size() == subsetKeys.size(),
                        "passed assignment wasnt complete ",
                        assignment
                );

                AssignmentInstance cai = cae.getAssignment();
                // we have conditional and unconditional nodes in the DAG
                DehydratedAssignment caiAssign;
                DehydratedAssignment caiParent;
                if (factorGroup.factorParentsNameAsSet.isEmpty()) {
                    caiAssign = dehydrate(cai);
                    caiParent = null;
                } else {
                    caiAssign = dehydrate(cai.difference(factorGroup.getFactorParentsNameAsSet()));
                    caiParent = dehydrate(cai.subset(factorGroup.getFactorParentsNameAsSet()));
                    Preconditions.checkState(!caiParent.isEmpty(), "trying to count conditional with empty parent");
                }

                if (cae.isFullyObserved()) {
                    factorGroup.countingFactor.increment(caiAssign, caiParent);
                } else {
                    // we're counting the conditional distribution so add an amount proportional to our
                    // certainty for this particular entry in the conditional distribution
                    // P(X | Y) = P(X ^ Y) / P(Y)
                    // we're treating X & Y as independent here which i agree is a little non-sensical given that
                    // we're going to all of this trouble to model the dependencies
                    double prob = 1.0;
                    for (String yCondX : factorGroup.factorNonParentsName) {
                        prob *= cae.certaintyOfVariable(yCondX);
                    }
                    factorGroup.countingFactor.add(caiAssign, caiParent, prob);
                }
            }
        }
        return assignment;
    }

    private void updateStats(CountAssignment assignment) {
        for (Entry<String, FactorStats> entry : factorStatsMap.entrySet()) {
            Object maybeVal = assignment.valueFor(entry.getKey());
            entry.getValue().onAssignment(maybeVal);
        }
    }

    public synchronized void printTo(PrintWriter pw) {
        pw.println(" ********* Count DAG ********* ");
        for (Entry<String, Collection<String>> entry : parents.asMap().entrySet()) {
            pw.println(entry.getKey() + " <- " + PrintUtil.commaJoiner.join(entry.getValue()));
        }
        pw.println();
        pw.println(" ********* Factor Stats ********* ");
        factorStatsMap.values().forEach(s -> s.printTo(pw));

        pw.println();
        pw.println(" ********* Count Results ********* ");
        factorGroups.values().forEach(g -> g.printTo(pw));
    }

    private void throwIfFrozen() {
        Preconditions.checkState(!isFrozen, "cannot mutate DAG once its frozen");
    }

    private void throwIfNotFrozen() {
        Preconditions.checkState(isFrozen, "cannot start counting until youve built the DAG and it is frozen");
    }

    public class FactorGroup implements Serializable {

        private static final long serialVersionUID = -7380442531302079327L;

        private final String name;
        private final CountFactor countingFactor;
        private final ImmutableList<String> factorParentsName;
        private final ImmutableList<String> factorNonParentsName;
        private final ImmutableSet<String> assignmentKeys;
        private final ImmutableSet<String> factorParentsNameAsSet; // just a set version of the factorParentsName list

        public FactorGroup(String name,
                CountFactor countingFactor,
                List<String> factorParentsName,
                List<String> factorNonParentsName,
                Set<String> assignmentKeys
        ) {
            this.name = name;
            this.countingFactor = countingFactor;
            this.factorParentsName = ImmutableList.copyOf(factorParentsName);
            this.factorNonParentsName = ImmutableList.copyOf(factorNonParentsName);
            this.assignmentKeys = ImmutableSet.copyOf(assignmentKeys);
            this.factorParentsNameAsSet = ImmutableSet.copyOf(Sets.newHashSet(factorParentsName));
        }

        public String getName() {
            return name;
        }

        public CountFactor getCountingFactor() {
            return countingFactor;
        }

        public List<String> getFactorParentsName() {
            return factorParentsName;
        }

        public Set<String> getFactorParentsNameAsSet() {
            return factorParentsNameAsSet;
        }

        public List<String> getFactorNonParentsName() {
            return factorNonParentsName;
        }

        public Set<String> getAssignmentKeys() {
            return assignmentKeys;
        }

        public <T> MutableMultinomial<T> makeUnconditionalUnaryCopy() {
            Preconditions.checkState(factorParentsName.isEmpty(),
                    "cant make a conditional for a group thats not conditional"
            );
            Preconditions.checkState(factorNonParentsName.size() == 1, "cant do unary with more than one assignment");
            return (MutableMultinomial<T>) vocabHydrater.hydrateMultinomialToUnary(countingFactor.unconditional, factorNonParentsName.get(0));
        }

        public <T> Map<AssignmentInstance, MutableMultinomial<T>> makeConditionalCopy() {
            Preconditions.checkState(!factorParentsName.isEmpty(),
                    "cant make a conditional for a group thats not conditional"
            );
            Map<DehydratedAssignment, MutableMultinomial<DehydratedAssignment>> input = countingFactor.conditional;
            Map<AssignmentInstance, MutableMultinomial<T>> result = Maps.newHashMapWithExpectedSize(input.size());
            int targetCount = factorNonParentsName.size();
            for (Entry<DehydratedAssignment, MutableMultinomial<DehydratedAssignment>> entry : input.entrySet()) {
                AssignmentInstance instance = vocabHydrater.hydrate(entry.getKey());
                if (targetCount == 1) {
                    result.put(instance,
                            (MutableMultinomial<T>) vocabHydrater.hydrateMultinomialToUnary(entry.getValue(), factorNonParentsName.get(0))
                    );
                } else {
                    result.put(instance,
                            (MutableMultinomial<T>) vocabHydrater.hydrateMultinomialToList(entry.getValue(), factorNonParentsName)
                    );
                }
            }
            return result;
        }

        public void printTo(PrintWriter pw) {
            pw.println("**** Factor Group " + name + " P( " + PrintUtil.commaJoiner.join(factorNonParentsName) +
                    " | " + PrintUtil.commaJoiner.join(factorParentsName) + " ) ****");
            this.countingFactor.makeJoint().printTo(pw, CountDag.this::hydrate);
        }

        @Override
        public String toString() {
            return "FactorGroup{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    // the coordinates and info of a sensitive distribution that needs deident work
    public static class SensitiveDistrib implements Serializable {

        private static final long serialVersionUID = 4039249048396016660L;

        public final String distribName;
        @Nullable public final String subFieldName;
        public final Vocabulary<Object> vocab;
        public final VocabCounter<Object> counter;

        public SensitiveDistrib(String distribName,
                String subFieldName,
                Vocabulary<Object> vocab,
                VocabCounter<Object> counter
        ) {
            this.distribName = distribName;
            this.subFieldName = subFieldName;
            this.vocab = vocab;
            this.counter = counter;
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        vocabHydrater = new VocabHydrater(this.vocabRegistry);
    }
}
