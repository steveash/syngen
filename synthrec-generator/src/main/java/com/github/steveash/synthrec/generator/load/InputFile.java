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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.steveash.synthrec.data.InvalidDataException;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.Record;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Closeables;

/**
 * @author Steve Ash
 */
public class InputFile implements Iterable<Record> {

    private final MapperFactory factory;
    private final InputConfig config;

    @Autowired
    public InputFile(MapperFactory factory, InputConfig config) {
        this.factory = factory;
        this.config = config;
    }

    public InputConfig getConfig() {
        return config;
    }

    @Override
    public Iterator<Record> iterator() {
        try {
            CharSource source = ReadWrite.findResource(config.getResource());
            final BufferedReader reader = source.openBufferedStream();
            String headerLine = reader.readLine();
            BiFunction<String, Integer, Record> mapper = factory.mapperFor(config, headerLine);
            return new AbstractIterator<Record>() {

                private int lineNo = 0;
                @Override
                protected Record computeNext() {
                    try {
                        while (true) {
                            String line = reader.readLine();
                            lineNo += 1;
                            if (line == null) {
                                Closeables.closeQuietly(reader);
                                return endOfData();
                            }
                            if (isBlank(line)) {
                                continue; // blank line try again
                            }
                            return mapper.apply(line, lineNo);
                        }
                    } catch (IOException e) {
                        throw new InvalidDataException("Problem with data input file " + config.getResource(), e);
                    }
                }
            };
        } catch (IOException e) {
            throw new InvalidDataException("Problem with data input file " + config.getResource(), e);
        }
    }
}
