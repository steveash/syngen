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

package com.github.steveash.synthrec.sampling;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.graph.Graph;

/**
 * @author Steve Ash
 */
public class GraphAlgs {

    /**
     * Returns the individual subgraphs that are partitionable (i.e. no edges exist between subgraphs)
     * and for each partition the nodes are listed in topological sort order.
     * @param dag
     * @param <T>
     * @return
     */
    public static <T> List<List<T>> topologicalRoots(Graph<T> dag) {
        List<T> topoSort = topologicalSort(dag);
        List<List<T>> results = Lists.newArrayList();
        Set<T> colored = Sets.newHashSet();
        for (T node : dag.nodes()) {
            if (colored.contains(node)) {
                continue;
            }
            Set<T> selected = Sets.newHashSet();
            colorFrom(dag, node, selected);
            Preconditions.checkState(Sets.intersection(selected, colored).isEmpty(), "overlapping partitions");
            results.add(topoSort.stream()
                    .filter(selected::contains)
                    .collect(Collectors.toList()));
            colored.addAll(selected);
        }
        return results;
    }

    private static <T> void colorFrom(Graph<T> dag, T node, Set<T> selectedThisTime) {
        if (selectedThisTime.contains(node)) {
            return; // already colored this
        }
        selectedThisTime.add(node);
        for (T beforeNode : dag.predecessors(node)) {
            colorFrom(dag, beforeNode, selectedThisTime);
        }
        for (T afterNode : dag.successors(node)) {
            colorFrom(dag, afterNode, selectedThisTime);
        }
    }

    public static <T> List<T> topologicalSort(Graph<T> dag) {
        LinkedList<T> result = Lists.newLinkedList();
        Set<T> tempMark = Sets.newHashSet();
        Set<T> mark = Sets.newHashSet();
        for (T node : dag.nodes()) {
            if (mark.contains(node)) {
                continue;
            }
            visitTopoSort(node, dag, tempMark, mark, result);
        }
        return result;
    }

    private static <T> void visitTopoSort(T node,
            Graph<T> graph,
            Set<T> tempMark,
            Set<T> mark,
            LinkedList<T> result
    ) {
        Preconditions.checkState(!tempMark.contains(node), "cycle detected");
        if (mark.contains(node)) return;
        tempMark.add(node);
        for (T child : graph.successors(node)) {
            visitTopoSort(child, graph, tempMark, mark, result);
        }
        mark.add(node);
        tempMark.remove(node);
        result.addFirst(node);
    }
}
