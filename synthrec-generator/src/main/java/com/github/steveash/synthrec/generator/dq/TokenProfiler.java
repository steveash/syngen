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

import com.github.steveash.jg2p.util.Percent;
import com.github.steveash.synthrec.canonical.MinimalNormalizer;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.stat.ConcurrentCounter;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

/**
 * Does token level profiling
 * @author Steve Ash
 */
public class TokenProfiler implements FieldProfiler {

    private static final String DATA_QUAL_FILE = "data-token-stats.csv";
    private static final Joiner CSV_JOINER = Joiner.on(',');

    private static final Splitter WORD_SPLITTER = Splitter.on(CharMatcher.whitespace())
            .omitEmptyStrings()
            .trimResults();

    private final String name;
    private final List<String> fields;
    private final ConcurrentCounter<String> counter = new ConcurrentCounter<>();

    public TokenProfiler(String name, List<String> fields) {
        this.name = name;
        this.fields = fields;
    }

    @Override
    public void onValue(ReadableRecord record) {
        for (String field : fields) {
            String value = record.getField(field, "");
            if (isBlank(value)) {
                continue;
            }
            value = MinimalNormalizer.INSTANCE.normalize(value);
            for (String token : WORD_SPLITTER.split(value)) {
                counter.increment(token);
            }
        }
    }

    @Override
    public void finish(Context context) {
        MutableMultinomial<String> multi = counter.drainTo();
        counter.clear();
        ReadWrite.writeCountTable(multi,
                new File(context.getOutputDir(), context.getMoniker() + "." + name + ".tokenvalues.psv"),
                "|"
        );
        MutableMultinomial<String> normalMulti = multi.normalizedCopy();
        File statsFile = new File(context.getOutputDir(), DATA_QUAL_FILE);
        boolean emitStatsHeader = !statsFile.exists();
        String moniker = context.getMoniker();
        long timestamp = context.getRunTime().getTime();
        try (PrintWriter statsPw = new PrintWriter(new FileOutputStream(statsFile, true))) {
            if (emitStatsHeader) {
                String[] header = {"moniker", "stamp", "token", "entropy", "nentropy", "diversity2",
                        "diversityp1", "diversityp50", "sum", "max", "min",
                        "distinct", "countAtMin", "uniqueness"};
                statsPw.println(CSV_JOINER.join(header));
            }

            String[] stats = {
                    moniker, moniker + timestamp,
                    name,
                    Double.toString(normalMulti.entropy()),
                    Double.toString(normalMulti.entropyPercOfMax()),
                    Double.toString(normalMulti.diversity(2.0)),
                    Double.toString(normalMulti.diversity(0.1)),
                    Double.toString(normalMulti.diversity(0.5)),
                    Double.toString(multi.sum()),
                    Double.toString(multi.maxValue()),
                    Double.toString(multi.minValue()),
                    Long.toString(multi.size()),
                    Double.toString(multi.countWithMinValue()),
                    Double.toString(Percent.value(multi.countWithMinValue(), multi.size()))
            };

            statsPw.println(CSV_JOINER.join(stats));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
}
