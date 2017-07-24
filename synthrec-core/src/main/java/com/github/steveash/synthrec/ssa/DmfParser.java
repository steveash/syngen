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

package com.github.steveash.synthrec.ssa;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

import com.github.steveash.synthrec.ssa.DmfParser.DmfRecord;
import com.google.common.base.Charsets;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.FluentIterable;
import com.google.common.io.CharSource;
import com.google.common.io.Closer;
import com.google.common.io.Files;

/**
 * Parser for the SSA death master file
 * @author Steve Ash
 */
public class DmfParser implements Closeable, Iterable<DmfRecord> {

    private final Closer closer = Closer.create();
    private final List<File> files;

    public DmfParser(List<File> files) {this.files = files;}

    @Override
    public void close() throws IOException {
        closer.close();
    }

    @Override
    public Iterator<DmfRecord> iterator() {
        return FluentIterable.from(files)
                .transformAndConcat(this::fileIble)
                .iterator();
    }

    private Iterable<DmfRecord> fileIble(File file) {
        return () -> {
            try {
                CharSource source = Files.asCharSource(file, Charsets.UTF_8);
                BufferedReader reader = source.openBufferedStream();
                closer.register(reader);
                return new AbstractIterator<DmfRecord>() {
                    @Override
                    protected DmfRecord computeNext() {
                        try {
                            String line = reader.readLine();
                            if (line == null) {
                                reader.close();
                                return endOfData();
                            }
                            return parse(line);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                };
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    static DmfRecord parse(String line) {
        // 185225219RICHARDS            SR  LEROY          G               1202199502151930
        // 067037273OCONNOR                 HELEN                         V1200199606291914
        try {
            return ImmutableDmfRecord.builder()
                    .id(line.substring(1, 10))
                    .familyName(line.substring(10, 30).trim())
                    .suffix(line.substring(30, 34).trim())
                    .givenName(line.substring(34, 49).trim())
                    .middleName(line.substring(49, 64).trim())
//                    .deathDate(LocalDate.of(
//                            Integer.parseInt(line.substring(69, 73)),
//                            Integer.parseInt(line.substring(65, 67)),
//                            Integer.parseInt(line.substring(67, 69))
//                    ))
//                    .birthDate(LocalDate.of(
//                            Integer.parseInt(line.substring(77, 81)),
//                            Integer.parseInt(line.substring(73, 75)),
//                            Integer.parseInt(line.substring(75, 77))
//                    ))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Problem parsing line:\n" + line, e);
        }
    }

    @Value.Immutable
    public static abstract class DmfRecord {
        public abstract String getId();

        public abstract String getGivenName();

        public abstract String getMiddleName();

        public abstract String getFamilyName();

        public abstract String getSuffix();
        @Nullable
        public abstract LocalDate getBirthDate();
        @Nullable
        public abstract LocalDate getDeathDate();
    }
}
