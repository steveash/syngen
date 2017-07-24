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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

/**
 * @author Steve Ash
 */
public class GraphAlgsTest {

    @Test
    public void shouldTopologicalRoots() throws Exception {
        MutableGraph<String> graph = GraphBuilder.directed().build();
        graph.addNode("A");
        graph.addNode("B");
        graph.addNode("C");
        graph.addNode("D");
        graph.addNode("E");
        graph.addNode("F");
        graph.addNode("G");
        graph.putEdge("B", "A");
        graph.putEdge("D", "C");
        graph.putEdge("D", "B");
        graph.putEdge("E", "F");
        List<List<String>> roots = GraphAlgs.topologicalRoots(graph);
        assertEquals(3, roots.size());
        assertEquals(Lists.newArrayList("D", "C", "B", "A"), roots.get(0));
        assertEquals(Lists.newArrayList("E", "F"), roots.get(1));
        assertEquals(Lists.newArrayList("G"), roots.get(2));
    }

    @Test
    public void shouldTopologicalSort() throws Exception {
        MutableGraph<String> graph = GraphBuilder.directed().build();
        graph.addNode("A");
        graph.addNode("B");
        graph.addNode("C");
        graph.addNode("D");
        graph.addNode("E");
        graph.addNode("F");
        graph.addNode("G");
        graph.putEdge("B", "A");
        graph.putEdge("D", "C");
        graph.putEdge("D", "B");
        graph.putEdge("E", "F");
        List<String> sort = GraphAlgs.topologicalSort(graph);
        assertEquals(Lists.newArrayList("G", "E", "F", "D", "C", "B", "A"), sort);
    }
}