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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.function.Function.identity;
import static org.apache.commons.convert.Converters.convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.steveash.synthrec.data.CsvTable.Row;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.common.primitives.Ints;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Class the prevides a simple interface around csv files that can work with headers or no headers
 * allow access to contents by field name (case insensitive) or field index, does simple conversion
 * of field values to typical types
 * @author Steve Ash
 */
public class CsvTable implements Iterable<Row> {

    public interface Row {

        String getString(String header);

        String getString(int fieldIndex);

        int getInt(String header);

        int getInt(int fieldIndex);

        double getDouble(String header);

        double getDouble(int fieldIndex);

        long getLong(String header);

        long getLong(int fieldIndex);

        int rowNumber();
    }

    public static class Builder {
        private CharSource source;
        private char sep = ',';
        private boolean trimResults = true;
        private boolean hasHeaders = true;
        private boolean autoDetectSep = false;
        private int expectedFieldCount = 0;
        private int skipFirstLines = 0;
        private BiFunction<List<String>, Integer, List<String>> mismatchStrategy = (row, exp) -> row;

        Builder(CharSource source) {
            this.source = source;
        }

        public Builder withSeparator(char separator) {
            this.sep = separator;
            this.autoDetectSep = false;
            return this;
        }

        public Builder trimResults() {
            this.trimResults = true;
            return this;
        }

        public Builder hasHeaders() {
            this.hasHeaders = true;
            return this;
        }

        public Builder autoDetectSeparator() {
            autoDetectSep = true;
            return this;
        }

        public Builder skipFirst(int linesToSkip) {
            this.skipFirstLines = linesToSkip;
            return this;
        }

        public Builder noHeaders(int expectedFieldCount) {
            this.expectedFieldCount = expectedFieldCount;
            this.hasHeaders = false;
            return this;
        }

        public Builder resolveMismatchWith(BiFunction<List<String>, Integer, List<String>> strategy) {
            this.mismatchStrategy = strategy;
            return this;
        }

        public CsvTable build() {
            if (autoDetectSep) {
                Preconditions.checkState(hasHeaders, "cant use autodetect if there are no headers");
                sep = guessSep();
            }
            Splitter splitter = Splitter.on(sep);
            if (trimResults) {
                splitter = splitter.trimResults();
            }
            Object2IntOpenHashMap<String> headers = null;
            List<String> headerList = null;
            if (hasHeaders) {
                try {
                    String firstLine = readHeaderLine(source, skipFirstLines);
                    Preconditions.checkNotNull(firstLine, "No line exists in file", source);

                    List<String> headerList2 = splitter.splitToList(firstLine);
                    headers = new Object2IntOpenHashMap<>(IntStream.range(0, headerList2.size())
                            .boxed()
                            .collect(Collectors.toMap(i -> headerList2.get(i).toLowerCase(), identity())));
                    headerList = headerList2;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                this.expectedFieldCount = headerList.size();
            }
            return new CsvTable(source,
                    splitter,
                    headers,
                    headerList,
                    expectedFieldCount,
                    skipFirstLines,
                    mismatchStrategy
            );
        }

        private char guessSep() {
            try {
                String firstLine = readHeaderLine(source, skipFirstLines);
                Preconditions.checkNotNull(firstLine, "No line exists in file", source);
                int commaCount = CharMatcher.is(',').countIn(firstLine);
                int pipeCount = CharMatcher.is('|').countIn(firstLine);
                int tabCount = CharMatcher.is('\t').countIn(firstLine);
                if (commaCount > 0 && pipeCount == 0 && tabCount == 0) {
                    return ',';
                }
                if (commaCount == 0 && pipeCount > 0 && tabCount == 0) {
                    return '|';
                }
                if (commaCount == 0 && pipeCount == 0 && tabCount > 0) {
                    return '\t';
                }
                throw new IllegalStateException("Cannot autodetect the separator from the header line: " + firstLine +
                        " for source file " + source);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private String readHeaderLine(CharSource source, int skipFirstLines) throws IOException {
            try (BufferedReader reader = source.openBufferedStream()) {
                for (int i = 0; i < skipFirstLines; i++) {
                    reader.readLine();
                }
                return reader.readLine();
            }
        }
    }

    public static Builder loadResource(String resourceName) {
        return new Builder(ReadWrite.findResource(resourceName));
    }

    public static Builder loadSource(CharSource source) {
        return new Builder(source);
    }

    public static Builder loadFile(File source) {
        return new Builder(Files.asCharSource(source, Charsets.UTF_8));
    }

    private final CharSource source;
    private final Splitter splitter;
    private final Object2IntOpenHashMap<String> headers; // headers lowercased here
    private final List<String> headersList; // headers as is here
    private final int expectedFieldCount;
    private final int skipLines;
    private final BiFunction<List<String>, Integer, List<String>> mismatchStrategy;

    private int knownRowCount = -1;

    private CsvTable(CharSource source,
            Splitter splitter,
            Object2IntOpenHashMap<String> headers,
            List<String> headerList,
            int expectedFieldCount,
            int skipLines,
            BiFunction<List<String>, Integer, List<String>> mismatchStrategy
    ) {
        this.source = source;
        this.splitter = splitter;
        this.headers = headers;
        this.headersList = headerList;
        this.expectedFieldCount = expectedFieldCount;
        this.skipLines = skipLines;
        this.mismatchStrategy = mismatchStrategy;
    }

    public List<String> getHeaders() {
        return this.headersList;
    }

    public Optional<Integer> estimateRowCount() {
        if (knownRowCount >= 0) {
            return Optional.of(knownRowCount);
        }
        Long streamLen = source.lengthIfKnown().or(-1L);
        long rowCount = 0;
        long sumLenCount = 0;
        try (BufferedReader reader = source.openBufferedStream()) {
            skipLines(reader);
            while (rowCount < 1000) {
                String line = reader.readLine();
                if (line == null) {
                    // we reached the end of the file so we have a precise answer
                    return Optional.of(Ints.saturatedCast(rowCount));
                }
                rowCount += 1;
                sumLenCount += line.length();
            }
            // read first few, let's estimate
            if (streamLen < 0) {
                // cant know underlying stream length...
                return Optional.empty();
            }
            double rowAvg = ((double) sumLenCount) / ((double) rowCount);
            long result = (long) (streamLen / rowAvg);
            return Optional.of(Ints.saturatedCast(result));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void skipLines(BufferedReader reader) throws IOException {
        for (int i = 0; i < skipLines; i++) {
            reader.readLine();
        }
    }

    @Override
    public Iterator<Row> iterator() {
        try {
            BufferedReader reader = source.openBufferedStream();
            skipLines(reader);
            if (headers != null) {
                reader.readLine(); // skip header
            }
            return new AbstractIterator<Row>() {
                private int rowId = headers == null ? 0 : 1; // start after header

                @Override
                protected Row computeNext() {
                    try {
                        String line = reader.readLine();
                        if (line == null) {
                            reader.close();
                            if (knownRowCount < 0) {
                                // we read through once so go ahead and set the count
                                knownRowCount = (headers == null ? rowId : rowId - 1);
                            }
                            return endOfData();
                        }
                        List<String> fields = splitter.splitToList(line);
                        if (fields.size() != expectedFieldCount) {
                            fields = mismatchStrategy.apply(fields, expectedFieldCount);
                            if (fields.size() != expectedFieldCount) {
                                throw new IllegalArgumentException("Row index " + rowId + " has more field values " +
                                        "than expected. Expected " + expectedFieldCount + " but had " + fields);
                            }
                        }
                        RowImpl row = new RowImpl(rowId, headers, fields);
                        rowId += 1;
                        return row;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            };
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public DoubleArrayList columnAsDouble(String colHeader) {
        Optional<Integer> rows = estimateRowCount();
        DoubleArrayList result = new DoubleArrayList(rows.orElse(32));
        int fieldIndex = resolveHeaderIndex(colHeader, headers);
        for (Row row : this) {
            result.add(row.getDouble(fieldIndex));
        }
        return result;
    }

    public List<String> columnAsString(String colHeader) {
        Optional<Integer> rows = estimateRowCount();
        List<String> result = new ArrayList<>(rows.orElse(32));
        int fieldIndex = resolveHeaderIndex(colHeader, headers);
        for (Row row : this) {
            result.add(row.getString(fieldIndex));
        }
        return result;
    }

    public IntArrayList columnAsInt(String colHeader) {
        Optional<Integer> rows = estimateRowCount();
        int fieldIndex = resolveHeaderIndex(colHeader, headers);
        IntArrayList result = new IntArrayList(rows.orElse(32));
        for (Row row : this) {
            result.add(row.getInt(fieldIndex));
        }
        return result;
    }

    public Iterable<String> columnsAsStringIter(int colIndex) {
        return Iterables.transform(this, r -> r.getString(colIndex));
    }

    private static int resolveHeaderIndex(String header, Map<String, Integer> headers) {
        checkArgument(headers != null, "cant use header based access methods when file had no headers");
        return checkNotNull(headers.get(header.toLowerCase()), "no header %s", header);
    }

    private static class RowImpl implements Row {

        private final int rowNumber;
        private final Map<String, Integer> headers; // headers lowercased here
        private final List<String> fields;

        private RowImpl(int rowNumber, Map<String, Integer> headers, List<String> fields) {
            this.rowNumber = rowNumber;
            this.headers = headers;
            this.fields = fields;
        }

        @Override
        public String getString(String header) {
            int fieldIndex = resolveHeaderIndex(header, headers);
            return fields.get(fieldIndex);
        }

        @Override
        public String getString(int fieldIndex) {
            Preconditions.checkArgument(fieldIndex < fields.size(),
                    "index %s exceeds size %s",
                    fieldIndex,
                    fields.size()
            );
            return fields.get(fieldIndex);
        }

        @Override
        public int getInt(String header) {
            return getAndFormat(header, Integer.class);
        }

        @Override
        public int getInt(int fieldIndex) {
            return getAndFormat(fieldIndex, Integer.class);
        }

        @Override
        public double getDouble(String header) {
            return getAndFormat(header, Double.class);
        }

        @Override
        public double getDouble(int fieldIndex) {
            return getAndFormat(fieldIndex, Double.class);
        }

        @Override
        public long getLong(String header) {
            return getAndFormat(getString(header), Long.class);
        }

        @Override
        public long getLong(int fieldIndex) {
            return getAndFormat(fieldIndex, Long.class);
        }

        private <T> T getAndFormat(String header, Class<T> targetClass) {
            try {
                return convert(getString(header), targetClass);
            } catch (Exception e) {
                throw new IllegalArgumentException("Problem parsing " + targetClass + " value for " +
                        header + " from row: " + this, e);
            }
        }

        private <T> T getAndFormat(int index, Class<T> targetClass) {
            try {
                return convert(getString(index), targetClass);
            } catch (Exception e) {
                throw new IllegalArgumentException("Problem parsing " + targetClass + " value for index " +
                        index + " from row: " + this, e);
            }
        }

        public int rowNumber() {
            return rowNumber;
        }

        @Override
        public String toString() {
            return "RowImpl{" +
                    "row=" + rowNumber +
                    ",fields=" + fields +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CsvTable{" +
                "source=" + source +
                '}';
    }
}
