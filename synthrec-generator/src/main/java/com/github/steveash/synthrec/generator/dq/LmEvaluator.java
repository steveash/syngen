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

package com.github.steveash.synthrec.generator.dq;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.generator.dq.FieldModel.ScoreResult;
import com.github.steveash.synthrec.generator.dq.FieldProfiler.Context;
import com.github.steveash.synthrec.stat.ConcurrentHistogram;
import com.github.steveash.synthrec.stat.ConcurrentHistogram.HistogramBin;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
public class LmEvaluator {
    private static final Logger log = LoggerFactory.getLogger(LmEvaluator.class);

    private static final String DATA_QUAL_FILE = "data-lm-stats.csv";
    private static final Joiner CSV_JOINER = Joiner.on(',');
    private static final int BIN_COUNT = 300;
    private static final double MAX_PERPLEXITY = 3.0;

    private final boolean measureLm;
    private final String fileBaseName;
    private final FieldModel lm;
    private final ConcurrentHistogram lmCounter;
    private final String[] examples;
    private final DoubleAdder numeratorAdder = new DoubleAdder();
    private final LongAdder denomAdder = new LongAdder();

    public LmEvaluator(boolean measureLm, String fileBaseName) {
        this.fileBaseName = fileBaseName;
        FieldModel loadedLm = null;
        String[] examples = null;
        if (measureLm) {
            try {
                loadedLm = ReadWrite.objectFromGzip("lms/LM-" + fileBaseName + ".dat", FieldModel.class);
            } catch (Exception e) {
                log.warn("Cannot find a language model for " + fileBaseName + " skipping this evaluation: " + e.getMessage());
                measureLm = false;
            }
            examples = new String[BIN_COUNT];
        }
        this.measureLm = measureLm;
        this.lm = loadedLm;
        this.lmCounter = new ConcurrentHistogram(0.0, MAX_PERPLEXITY, BIN_COUNT);
        this.examples = examples;
    }

    public void onValue(String value) {
        if (!measureLm) return;
        ScoreResult result = lm.score(value);
        lmCounter.add(result.perplexity);
        int binIndex = lmCounter.convertSampleToBinIndex(result.perplexity);
        examples[binIndex] = value;
        numeratorAdder.add(result.perpNum);
        denomAdder.add(result.perDenom);
    }

    public void writeOut(Context context) {
        if (!measureLm) return;

        File statsFile = new File(context.getOutputDir(), DATA_QUAL_FILE);
        boolean emitStatsHeader = !statsFile.exists();
        String moniker = context.getMoniker();
        long timestamp = context.getRunTime().getTime();
        try (PrintWriter statsPw = new PrintWriter(new FileOutputStream(statsFile, true))) {
            if (emitStatsHeader) {
                List<String> header = Lists.newArrayList("moniker",
                        "stamp",
                        "type",
                        "sumperp",
                        "sumtoks",
                        "perplexity"
                );
                Iterator<HistogramBin> iter = lmCounter.iterator();
                while (iter.hasNext()) {
                    HistogramBin bin = iter.next();
                    header.add(String.format("%.3f", bin.binMinimumInclusive));
                }
                statsPw.println(CSV_JOINER.join(header));
            }

            List<String> stats = Lists.newArrayList(
                    moniker,
                    moniker + timestamp,
                    fileBaseName,
                    Double.toString(this.numeratorAdder.sum()),
                    Long.toString(this.denomAdder.sum()),
                    Double.toString(this.numeratorAdder.sum() / this.denomAdder.sum())
            );
            Iterator<HistogramBin> iter = lmCounter.iterator();
            while (iter.hasNext()) {
                HistogramBin bin = iter.next();
                stats.add(String.format("%d", bin.count));
            }
            statsPw.println(CSV_JOINER.join(stats));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        File examplesFile = new File(context.getOutputDir(), context.getMoniker() + "." + fileBaseName + "-examples.txt");
        try (PrintWriter expw = new PrintWriter(new FileOutputStream(examplesFile))) {
            expw.println("bin|seq|ppx|example");
            for (int i = 0; i < examples.length; i++) {
                String example = examples[i];
                if (example == null) continue;
                expw.println(
                        Integer.toString(i) + "|" +
                                "0|" +
                                lmCounter.getRangeLabelAtIndex(i) + "|" +
                                example
                );
            }
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        lmCounter.clear();
    }
}
