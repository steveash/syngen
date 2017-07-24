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

package com.github.steveash.synthrec.generator.load;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.dsl.DslFactory;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.mapping.MappingAction;
import com.github.steveash.synthrec.mapping.MappingSpec;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;

/**
 * @author Steve Ash
 */
@LazyComponent
public class GroovyMapperFactory implements MapperFactory {

    private static final String DEFAULT_DELIM = ",";

    @Override
    public BiFunction<String, Integer, Record> mapperFor(InputConfig config, String headerLine) {
        Splitter splitter = Splitter.on(StringUtils.defaultIfBlank(config.getDelimiter(), DEFAULT_DELIM)).trimResults();
        List<String> headers = splitter.splitToList(headerLine);
        Map<String,Integer> headerToIndex = IntStream.range(0, headers.size())
                .boxed()
                .collect(Collectors.toMap(headers::get, i -> i));
        Preconditions.checkState(headers.size() == headerToIndex.size(), "duplicate headers", headerLine, headerToIndex);
        CharSource mappingConfig = ReadWrite.findResource(config.getMapping());
        MappingSpec spec = new MappingSpec();
        DslFactory.evaluate(mappingConfig, config.getMapping(), spec);
        ImmutableList<MappingAction> actions = ImmutableList.copyOf(spec.getActions());
        return (line, lineNo) -> {
            List<String> fields = splitter.splitToList(line);
            Record record = new Record(lineNo);
            for (MappingAction action : actions) {
                action.execute(headerToIndex, fields, record);
            }
            return record;
        };
    }
}
