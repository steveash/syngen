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

package com.github.steveash.synthrec.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.FluentIterable;
import com.google.common.io.CharSource;
import com.google.common.io.Closer;
import com.google.common.io.Files;

/**
 * @author Steve Ash
 */
public class SourceIterable implements Iterable<String> {

    public static final SourceIterable from(Closer closer, Charset charset, File file) {
        return new SourceIterable(Files.asCharSource(file, charset), closer, false);
    }

    public static final Iterable<String> fromMany(Closer closer, Charset charset, Iterable<File> files) {
        return FluentIterable
                .from(files)
                .transformAndConcat(f -> from(closer, charset, f));
    }

    public static final Iterable<String> fromMany(Closer closer, Charset charset, File... files) {
        return fromMany(closer, charset, Arrays.asList(files));
    }

    private final CharSource source;
    private final Closer closer;
    private final boolean skipFirstLine;

    public SourceIterable(CharSource source, Closer closer, boolean skipFirstLine) {
        this.source = source;
        this.closer = closer;
        this.skipFirstLine = skipFirstLine;
    }

    @Override
    public Iterator<String> iterator() {
        BufferedReader reader;
        try {
            reader = source.openBufferedStream();
            if (skipFirstLine) {
                reader.readLine(); // read first line to skip
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final BufferedReader finalReader = closer.register(reader);
        return new AbstractIterator<String>() {
            @Override
            protected String computeNext() {
                try {
                    String line = finalReader.readLine();
                    if (line != null) {
                        return line;
                    }
                    try {
                        finalReader.close();
                    } catch (IOException e1) {
                        throw new UncheckedIOException(e1);
                    }
                    return endOfData();
                } catch (IOException e) {
                    try {
                        finalReader.close();
                    } catch (IOException e1) {
                        e.addSuppressed(e1);
                    }
                    throw new UncheckedIOException(e);
                }
            }
        };
    }
}
