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

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.convert.Converters;

import com.github.steveash.synthrec.gen.OutputField;
import com.github.steveash.synthrec.gen.OutputFieldBuilder;
import com.github.steveash.synthrec.gen.RecordWriter;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.CharSink;

/**
 * Owns the output field configuration;
 * @author Steve Ash
 * @see com.github.steveash.synthrec.generator.profiling.count.CountDagService
 * @see GenRecordService
 */
@LazyComponent
public class RecordWriterService {

    private static final char DELIM = '|';

    @Resource private GenRecordsConfig genRecordsConfig;

    public RecordWriter createWriterForConfig(CharSink sink) {
        List<String> golds = genRecordsConfig.getGoldFields();
        Preconditions.checkState(!golds.isEmpty(), "there are no gold fields to output");

        Builder<OutputField> list = ImmutableList.builder();
        for (String field : golds) {

            OutputFieldBuilder ofb = OutputField.builder(field);
            ofb.setFormatter(DEFAULT_SINGLE);
            list.add(ofb.create());
        }
        return new RecordWriter(list.build(), DELIM, sink);
    }

//    public RecordWriter createWriterForGenFlow(GenDag flow, CharSink sink) {
//
//        Builder<OutputField> list = ImmutableList.builder();
//        List<GenNode> phases = flow.getGenPhases();
//        for (GenNode phase : phases) {
//            List<String> keys = phase.outputKeys();
//            OutputFieldBuilder ofb = OutputField.builder(keys.get(0));
//            ofb.setFormatter(DEFAULT_SINGLE);
//            list.add(ofb.create());
//        }
//        return new RecordWriter(list.build(), DELIM, sink);
//    }

    public static final Function<Object, String> DEFAULT_SINGLE = obj -> Converters.convert(obj, String.class);
}
