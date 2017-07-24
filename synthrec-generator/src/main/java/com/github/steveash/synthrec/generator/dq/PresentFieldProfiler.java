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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import com.github.steveash.jg2p.util.Percent;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.stat.ConcurrentCounter;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.string.TokenStringBuilder;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * @author Steve Ash
 */
public class PresentFieldProfiler implements FieldProfiler {

    private static final String DATA_QUAL_FILE = "data-missing-stats.csv";
    private static final Joiner CSV_JOINER = Joiner.on(',');

    private final ConcurrentCounter<String> counter = new ConcurrentCounter<>();
    private final ImmutableList<String> fields;
    private final LongAdder totalFields = new LongAdder();
    private final LongAdder presentFields = new LongAdder();
    private final LongAdder recordCount = new LongAdder();

    public PresentFieldProfiler(List<String> fields) {this.fields = ImmutableList.copyOf(fields);}

    @Override
    public void onValue(ReadableRecord record) {
        TokenStringBuilder sb = new TokenStringBuilder(" & ");
        recordCount.increment();
        for (String field : fields) {
            String result = record.getNormal(field, null);
            if (isNotBlank(result)) {
                sb.append(field);
                presentFields.increment();
            }
            totalFields.increment();
        }
        counter.increment(sb.toString());
    }

    @Override
    public void finish(Context context) {
        MutableMultinomial<String> multinomial = counter.drainTo();
        counter.clear();
        ReadWrite.writeCountTable(multinomial,
                new File(context.getOutputDir(), context.getMoniker() + ".presentfields.psv"),
                "|"
        );
        multinomial = multinomial.normalize();
        File statsFile = new File(context.getOutputDir(), DATA_QUAL_FILE);
        boolean emitStatsHeader = !statsFile.exists();
        String moniker = context.getMoniker();
        long timestamp = context.getRunTime().getTime();
        try (PrintWriter statsPw = new PrintWriter(new FileOutputStream(statsFile, true))) {
            if (emitStatsHeader) {
                String[] header = {"moniker", "stamp", "recordcount", "fieldcount", "presentcount",
                        "presence", "entropy", "nentropy"};
                statsPw.println(CSV_JOINER.join(header));
            }

            String[] stats = {
                    moniker,
                    moniker + timestamp,
                    Long.toString(this.recordCount.sum()),
                    Long.toString(this.totalFields.sum()),
                    Long.toString(this.presentFields.sum()),
                    Double.toString(Percent.value(this.presentFields.sum(), this.totalFields.sum())),
                    Double.toString(multinomial.entropy()),
                    Double.toString(multinomial.entropyPercOfMax())
            };
            statsPw.println(CSV_JOINER.join(stats));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
}