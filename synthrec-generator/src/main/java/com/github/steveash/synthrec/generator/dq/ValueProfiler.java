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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import com.github.steveash.jg2p.util.Percent;
import com.github.steveash.synthrec.canonical.MinimalNormalizer;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.stat.ConcurrentCounter;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * One field that you are profiling in the data quality evaluation
 * @author Steve Ash
 */
public class ValueProfiler implements FieldProfiler {

    private static final String DELIM = "|";
    private static final CharMatcher DELIM_MATCHER = CharMatcher.is(DELIM.charAt(0));
    private static final Joiner CSV_JOINER = Joiner.on(',');
    private static final String DATA_QUAL_FILE = "data-field-stats.csv";

    private final String fieldName;
    private final ConcurrentCounter<String> valCounter = new ConcurrentCounter<>();
    private final LongAdder missingCount = new LongAdder();
    private final LongAdder presentCount = new LongAdder();
    private final LongAdder normalizedCount = new LongAdder();
    private final LmEvaluator lmEvaluator;

    public ValueProfiler(boolean measureLm, String fieldName) {
        this.fieldName = fieldName;
        this.lmEvaluator = new LmEvaluator(measureLm, "values." + fieldName);
    }

    public final String getFieldName() {
        return fieldName;
    }

    public long getMissingCount() {
        return missingCount.sum();
    }

    public long getNormalizedCount() {
        return normalizedCount.sum();
    }

    public long getPresentCount() {
        return presentCount.sum();
    }

    public MutableMultinomial<String> makeValues() {
        return valCounter.drainTo();
    }

    public void clearValueCounter() {
        valCounter.clear();
    }

    public void onValue(ReadableRecord record) {
        String original = record.getField(fieldName, null);
        if (isBlank(original)) {
            missingCount.increment();
            return;
        }
        presentCount.increment();
        original = MinimalNormalizer.INSTANCE.normalize(original);
        valCounter.increment(original);
        lmEvaluator.onValue(original);
        String normalized = record.getField(fieldName, null);
        if (normalized == null || !original.equalsIgnoreCase(normalized)) {
            // this was normalized
            normalizedCount.increment();
        }
    }

    @Override
    public void finish(Context context) {

        if (this.getPresentCount() == 0) {
            return;
        }

        String moniker = context.getMoniker();
        long timestamp = context.getRunTime().getTime();

        File countsFile = new File(context.getOutputDir(), moniker + ".counts." + getFieldName() + ".psv");
        File valuesFile = new File(context.getOutputDir(), moniker + ".values." + getFieldName() + ".psv");
        MutableMultinomial<String> multi = this.makeValues();
        this.clearValueCounter();
        List<Entry<String>> entries = multi.rankedList();

//        ReadWrite.writeRankTable(entries, countsFile, DELIM);
        ReadWrite.writeCountTable(entries, valuesFile, DELIM);
        lmEvaluator.writeOut(context);

        File statsFile = new File(context.getOutputDir(), DATA_QUAL_FILE);
        boolean emitStatsHeader = !statsFile.exists();
        try (PrintWriter statsPw = new PrintWriter(new FileOutputStream(statsFile, true))) {
            double minValue = multi.minValue();
            double countWithMinValue = multi.countWithMinValue();
            multi.normalize(); // now normalize

            if (emitStatsHeader) {
                String[] header = {"moniker", "stamp", "field", "entropy", "nentropy", "diversity2",
                        "diversityp1", "diversityp50", "present", "missing", "normalized", "distinct",
                        "minMulti", "uniqueness", "missingness"};
                statsPw.println(CSV_JOINER.join(header));
            }
            long total = getPresentCount() + getMissingCount();
            String[] stats = {
                    moniker, moniker + timestamp, getFieldName(),
                    Double.toString(multi.entropy()),
                    Double.toString(multi.entropyPercOfMax()),
                    Double.toString(multi.diversity(2.0)),
                    Double.toString(multi.diversity(0.1)),
                    Double.toString(multi.diversity(0.5)),
                    Long.toString(this.getPresentCount()),
                    Long.toString(this.getMissingCount()),
                    Long.toString(this.getNormalizedCount()),
                    Long.toString(multi.size()),
                    Double.toString(minValue),
                    Double.toString(Percent.value(countWithMinValue, this.getPresentCount())),
                    Double.toString(Percent.value(getMissingCount(), total))
            };
            statsPw.println(CSV_JOINER.join(stats));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
}
