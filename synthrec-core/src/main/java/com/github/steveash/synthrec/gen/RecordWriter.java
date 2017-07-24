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

package com.github.steveash.synthrec.gen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSink;
import com.google.common.io.Closer;

/**
 * Owns writing records in a predefined format and any kind of formatters that need to be applied
 *
 * @author Steve Ash
 */
public class RecordWriter implements AutoCloseable {

    private final Closer closer = Closer.create();
    private final ImmutableList<OutputField> fields;
    private final String delim;
    private final String escapedDelim;
    private final Supplier<PrintWriter> pw;
    private final CharMatcher delimMatcher;

    public RecordWriter(ImmutableList<OutputField> fields, char delim, CharSink sink) {
        this.fields = fields;
        this.delim = Character.toString(delim);
        this.escapedDelim = " ";
        this.delimMatcher = CharMatcher.is(delim);
        this.pw = Suppliers.memoize( () -> {
            try {
                PrintWriter writer = new PrintWriter(sink.openBufferedStream());
                writer = closer.register(writer);
                writer.println(fields.stream()
                        .map(OutputField::getHeader)
                        .map(this::escape)
                        .collect(Collectors.joining(this.delim)));

                return writer;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ImmutableList<OutputField> getFields() {
        return fields;
    }

    protected String escape(String value) {
        return delimMatcher.replaceFrom(value, escapedDelim);
    }

    public void write(GenAssignment assignment) {
        PrintWriter pw = this.pw.get();
        pw.println(fields.stream()
                .map( fld -> fld.render(assignment))
                .map(this::escape)
                .collect(Collectors.joining(this.delim)));
    }

    @Override
    public void close() throws IOException {
        closer.close();
    }
}
