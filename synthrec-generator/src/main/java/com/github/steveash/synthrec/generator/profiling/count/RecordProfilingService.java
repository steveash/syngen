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

package com.github.steveash.synthrec.generator.profiling.count;

import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.steveash.synthrec.collect.VocabCounter;
import com.github.steveash.synthrec.collect.Vocabulary;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.FactorStats;
import com.github.steveash.synthrec.generator.load.InputFile;
import com.github.steveash.synthrec.generator.load.InputPipeline;
import com.github.steveash.synthrec.generator.profiling.count.CountDagService.DagAssigner;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.reducer.ValueReducer;
import com.github.steveash.synthrec.reducer.ValueReducerRegistry;
import com.github.steveash.synthrec.util.StreamCounter;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

/**
 * Actually orchestrates all of the counting and outputting for profiling
 * @author Steve Ash
 */
@LazyComponent
public class RecordProfilingService {
    private static final Logger log = LoggerFactory.getLogger(RecordProfilingService.class);

    @Resource private CountDagService countDagService;
    @Resource private InputPipeline inputPipeline;
    @Resource private ValueReducerRegistry valueReducerRegistry;
    @Resource private MetricRegistry registry;

    public CountDag execute(InputFile inputFile) {
        Stopwatch watch = Stopwatch.createStarted();
        log.info("Profiling records...");
        CountDag dag = countDagService.makeCountDag();
        DagAssigner assigner = countDagService.makeAssigner(dag);
        Meter meter = registry.meter("profilingInputs");

        long totalRecords = inputPipeline.processedFrom(inputFile)
                .parallel()
                .map(assigner::makeAssignmentFor)
                .map(dag::add)
                .map(new StreamCounter<>("Input record profiling"))
                .map(e -> {meter.mark(); return e;})
                .count();

        reduceMarkedFactors(dag);

        watch.stop();
        log.info("Counted {} records in {}", totalRecords, watch.toString());
        return dag;
    }

    private void reduceMarkedFactors(CountDag dag) {
        for (String factor : dag.getReduceFactors()) {
            ValueReducer reducer = valueReducerRegistry.reducerFor(factor);
            FactorStats stats = dag.getFactorStats(factor);
            if (stats.isSketches()) {
                reduceSubfields(reducer, stats);
            } else {
                VocabCounter<Object> vocabCounter = stats.getValueVocab();
                int count = reduceVocab(vocabCounter, reducer);
                log.debug("Reducing factor {} updated {} items", factor, count);
            }
        }
    }

    private void reduceSubfields(ValueReducer reducer, FactorStats stats) {
        Set<String> subFieldNames = stats.getSubFieldNames();
        for (String subFieldName : subFieldNames) {
            VocabCounter<Object> vocabCounter = stats.getSubFieldVocab(subFieldName);
            int count = reduceVocab(vocabCounter, reducer);
            log.debug("Reducing subfield {} updated {} items", subFieldName, count);
        }
    }

    private int reduceVocab(VocabCounter<Object> vocabCounter, ValueReducer reducer) {
        Vocabulary<Object> vocab = vocabCounter.getVocab();
        int count = 0;

        for (int i = 1; i < vocab.size(); i++) {
            Object maybe = vocab.getForIndex(i);
            if (maybe == null || !(maybe instanceof String)) {
                continue;
            }

            String input = (String) maybe;
            String reduced = reducer.reduceIfNecessary(input, vocabCounter.countByIndex(i));
            Preconditions.checkNotNull(reduced);
            if (!reduced.equals(input)) {
                vocab.updateIndexValue(i, reduced);
                count += 1;
            }
        }
        return count;
    }
}
