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

package com.github.steveash.synthrec.generator.gen;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.GenDag;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.OutputField;
import com.github.steveash.synthrec.gen.RecordWriter;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.io.CharSink;
import com.google.common.util.concurrent.RateLimiter;

/**
 * Orchestrates actually generating the records based on profiling information
 * @author Steve Ash
 */
@LazyComponent
public class GenRecordService {
    private static final Logger log = LoggerFactory.getLogger(GenRecordService.class);

    @Resource private GenRecordsConfig genRecordsConfig;
    @Resource private GenNodeRegistry genNodeRegistry;
    @Resource private RecordWriterService recordWriterService;

    public void generate(CountDag countDag, CharSink sink) throws IOException {
        int count = genRecordsConfig.getProduceCount();
        GenContext context = new GenContext(count);
        try (RecordWriter rws = recordWriterService.createWriterForConfig(sink)) {
            GenDag genDag = buildDag(rws, countDag);
            RateLimiter limiter = RateLimiter.create(0.5);
            log.info("Generating " + count + " synthetic ");
            for (int i = 0; i < count; i++) {
                GenAssignment assignment = genDag.generate(context);
                rws.write(assignment);
                if (i % 16 == 0) {
                    if (limiter.tryAcquire()) {
                        log.info("Generated " + i + " synthetic records...");
                    }
                }
            }
        }
        log.info("Generated " + count + " records.");
    }

    private GenDag buildDag(RecordWriter rws, CountDag countDag) {
        List<GenNode> nodes = Lists.newArrayList();
        log.info("Building generator graph...");
        Set<String> satisfied = Sets.newHashSet();
        Set<String> requires = Sets.newHashSet();
//        for (FactorGroup group : countDag.topologicalOrdering()) {
//            GenNode node = genNodeRegistry.makeNodeFor(group.getName(), countDag);
//            log.info(">> gen node from group " + group.getName());
//            satisfied.addAll(node.outputKeys());
//            requires.addAll(node.inputKeys());
//            nodes.add(node);
//        }
        log.info("Adding {} output fields...", rws.getFields().size());
        for (OutputField outputField : rws.getFields()) {
            if (satisfied.contains(outputField.getGenAssignKey())) {
                continue; // already being output from the count dag nodes
            }
            GenNode node = genNodeRegistry.makeNodeFor(outputField.getGenAssignKey(), countDag);
            log.info(">> gen node for output field " + GenNode.nodeToString(node));
            satisfied.addAll(node.outputKeys());
            requires.addAll(node.inputKeys());
            nodes.add(node);
        }

        log.info("Adding latent gen nodes...");
        SetView<String> missing = Sets.difference(requires, satisfied);
        while (!missing.isEmpty()) {
            String nextToGet = Iterables.getFirst(missing, null);
            GenNode node = genNodeRegistry.makeNodeFor(nextToGet, countDag);
            log.info(">> gen node for latent field " + GenNode.nodeToString(node));
            satisfied.addAll(node.outputKeys());
            requires.addAll(node.inputKeys());
            nodes.add(node);
            missing = Sets.difference(requires, satisfied);
        }
        return new GenDag(nodes, this.genRecordsConfig.getMaxRejectSamples());
    }
}
