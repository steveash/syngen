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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.sampling.GraphAlgs;
import com.github.steveash.synthrec.stat.RandUtil;
import com.github.steveash.synthrec.stat.ThreadLocalRandomGenerator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

/**
 * A directed acyclic graph for generating synthetic gold records; can be initialized with a CountDag as a starting
 * point and then more generative nodes added to go from sketches/simplified profiling info to real records
 * <p>
 * Owns the actual sampling process, rejection, and resampling
 * @author Steve Ash
 */
public class GenDag {
    private static final Logger log = LoggerFactory.getLogger(GenDag.class);

    private final ImmutableList<List<GenNode>> dagRoots;
    private final MutableGraph<GenNode> dag;
    private final int maxAttemptsToSample;
    private static final ThreadLocalRandomGenerator rand = RandUtil.threadLocalRand();

    public GenDag(Collection<GenNode> genPhases, int maxAttemptsToSample) {
        this.dag = buildGraph(genPhases);
        this.maxAttemptsToSample = maxAttemptsToSample;
        this.dagRoots = ImmutableList.copyOf(GraphAlgs.topologicalRoots(dag));
        throwIfInvalid();
    }

    private void throwIfInvalid() {
        for (List<GenNode> chain : dagRoots) {
            Set<String> satisfied = Sets.newHashSet();
            for (GenNode node : chain) {
                if (!satisfied.containsAll(node.inputKeys())) {
                    throw new IllegalStateException("The gen dag has a chain like " + GenNode.chainToString(chain) +
                            " and node " + GenNode.nodeToString(node) + " requires " + node.inputKeys() + " but the" +
                            " current satisfied dependencies in the chain are only " + satisfied);
                }
                satisfied.addAll(node.outputKeys());
            }
        }
    }

    public GenAssignment generate(GenContext context) {
        RandomGenerator localRand = rand.getThreadGenerator();
        MutableGenAssignment assign = new MutableGenAssignment();
        for (List<GenNode> rootChain : dagRoots) {
            GenAssignment partial = generateForChain(localRand, rootChain, context);
            OverridingGenAssignment.flattenTo(partial, assign);
        }
        return assign;
    }

    private GenAssignment generateForChain(RandomGenerator localRand, List<GenNode> rootChain, GenContext context) {
        outer:
        for (int i = 0; i < maxAttemptsToSample; i++) {
            MutableGenAssignment assignment = new MutableGenAssignment();
            for (GenNode node : rootChain) {
                try {
                    if (!node.sample(localRand, assignment, context)) {
                        // first just try to redo the whole chain; if this turns out to be terribly
                        // inefficient then we could create override assignments at each step in the
                        // chain and backoff one step in the chain at a time until we make progress (with
                        // some fail-safe to just start over entirely)
                        continue outer;
                    }
                } catch (TooManyRejectsSamplingException e) {
                    continue outer;
                } catch (RuntimeException e) {
                    throw new FailedSampleException(
                            "Unexpected problem generating a sample: " + e.getMessage() +
                                    "; chain " + rootChain + " and trying to run node " + node + " with the " +
                                    "given assignment: " + assignment, e);
                }
            }
            return assignment;
        }
        // i know this seems like it should be a TooManyRejects but this is the thing that owns that policy
        // and so this is a real failure at this point
        throw new FailedSampleException("Ran out of sampling attempts and never did generate a sample; tried " +
                maxAttemptsToSample);
    }

    private static MutableGraph<GenNode> buildGraph(Collection<GenNode> input) {
        MutableGraph<GenNode> graph = GraphBuilder.<Integer>directed()
                .allowsSelfLoops(false)
                .expectedNodeCount(input.size())
                .build();

        for (GenNode genNode : input) {
            graph.addNode(genNode);
        }
        for (GenNode child : input) {
            for (GenNode parent : input) {
                if (child == parent) {
                    continue;
                }
                if (needsAny(child, parent)) {
                    // parents point to children to get out current impl of topological sorting
                    graph.putEdge(parent, child);
                }
            }
        }
        return graph;
    }

    private static boolean needsAny(GenNode child, GenNode parent) {
        for (String parentout : parent.outputKeys()) {
            if (child.inputKeys().contains(parentout)) {
                return true;
            }
        }
        return false;
    }
}
