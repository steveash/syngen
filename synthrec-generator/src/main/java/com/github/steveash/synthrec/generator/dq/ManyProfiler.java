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
import java.util.List;

import com.github.steveash.synthrec.canonical.MinimalNormalizer;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.stat.ConcurrentCounter;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.string.TokenStringBuilder;

/**
 * For a list of fields -- this concats them together, transforms each token into a binned pattern
 * and then profiles that pattern
 * @author Steve Ash
 */
public class ManyProfiler implements FieldProfiler {

    protected final String name;
    protected final List<String> fields;
    private final ConcurrentCounter<String> counter = new ConcurrentCounter<>();
    private final LmEvaluator lmEvaluator;

    public ManyProfiler(boolean measureLm, String name, List<String> fields) {
        this.name = name;
        this.fields = fields;
        this.lmEvaluator = new LmEvaluator(measureLm, this.makeBaseName(name));
    }

    @Override
    public void onValue(ReadableRecord record) {
        TokenStringBuilder sb = new TokenStringBuilder();
        for (String field : fields) {
            String maybe = record.getField(field, null);
            if (isBlank(maybe)) {
                continue;
            }
            maybe = MinimalNormalizer.INSTANCE.normalize(maybe);
            sb.append(xform(maybe));
        }
        if (!sb.isEmpty()) {
            String value = sb.toString();
            counter.increment(value);
            lmEvaluator.onValue(value);
        }
    }

    // extension point for people to transform the value before aggregating together
    protected String xform(String maybe) {
        return maybe;
    }

    @Override
    public void finish(Context context) {
        ReadWrite.writeCountTable(makeValueMulti(), new File(context.getOutputDir(), makeOutputFileName(context)), "|");
        clearValueCounter();
        lmEvaluator.writeOut(context);
    }

    protected String makeBaseName(String name) {
        return name + ".manystrings";
    }

    protected String makeOutputFileName(Context context) {
        return context.getMoniker() + "." + makeBaseName(name) + ".psv";
    }

    public MutableMultinomial<String> makeValueMulti() {
        return counter.drainTo();
    }

    public void clearValueCounter() {
        counter.clear();
    }
}
