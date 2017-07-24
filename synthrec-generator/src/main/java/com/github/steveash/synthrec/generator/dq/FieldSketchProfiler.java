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
import java.util.List;

import com.github.steveash.kylm.model.immutable.ImmutableLM;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.FeatureComputer.FeatureKey;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.stat.ConcurrentCounter;
import com.github.steveash.synthrec.string.TokenStringBuilder;

/**
 * @author Steve Ash
 */
public class FieldSketchProfiler implements FieldProfiler {

    private final String name;
    private final String fileBaseName;
    private final LmEvaluator lmEvaluator;
    private final List<FeatureKey<FieldSketch>> fields;
    private final ConcurrentCounter<String> counter = new ConcurrentCounter<>();

    public FieldSketchProfiler(boolean measureLm, String name, List<FeatureKey<FieldSketch>> fields) {
        this.name = name;
        this.fileBaseName = name + ".structure";
        this.fields = fields;
        this.lmEvaluator = new LmEvaluator(measureLm, fileBaseName);
    }

    @Override
    public void onValue(ReadableRecord record) {
        TokenStringBuilder sb = new TokenStringBuilder();
        for (FeatureKey<FieldSketch> field : fields) {
            FieldSketch maybe = record.getFeature(field, null);
            if (maybe == null) {
                continue;
            }
            for (int i = 0; i < maybe.size(); i++) {
                sb.append(maybe.getComponentAsString(i));
            }
        }
        if (!sb.isEmpty()) {
            String value = sb.toString();
            counter.increment(value);
            lmEvaluator.onValue(value);
        }
    }

    @Override
    public void finish(Context context) {
        ReadWrite.writeCountTable(counter.drainTo(), new File(context.getOutputDir(), context.getMoniker() + "." + fileBaseName + ".psv"), "|");
        counter.clear();
        lmEvaluator.writeOut(context);
    }
}
