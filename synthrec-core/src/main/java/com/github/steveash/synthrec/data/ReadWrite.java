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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;

import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * @author Steve Ash
 */
public class ReadWrite {

    private static final int GZIP_BUFFER = 8 * 1024;
    private static final Joiner SPACE_JOINER = Joiner.on(' ');

    public static <T> T objectFrom(String resourceName, Class<T> clazz) {
        ByteSource src = findByteResource(resourceName);
        return readObjectFromByteSource(src);
    }

    public static <T> T objectFromFile(File inputFile, Class<T> clazz) {
        ByteSource src = Files.asByteSource(inputFile);
        return readObjectFromByteSource(src);
    }

    private static <T> T readObjectFromByteSource(ByteSource src) {
        try (ObjectInputStream ois = new ObjectInputStream(src.openBufferedStream())) {
            return (T) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void objectToGzip(Object object, File outputFile) {
        ByteSink sink = Files.asByteSink(outputFile);
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new GZIPOutputStream(sink.openBufferedStream(), GZIP_BUFFER))) {

            oos.writeObject(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T objectFromGzip(String resourceName, Class<T> clazz) {
        ByteSource src = findByteResource(resourceName);
        try (ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(src.openBufferedStream(),
                GZIP_BUFFER
        ))) {
            return (T) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void objectToFile(Object object, File outputFile) {
        ByteSink sink = Files.asByteSink(outputFile);
        try (ObjectOutputStream oos = new ObjectOutputStream(sink.openBufferedStream())) {
            oos.writeObject(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Stream<String> linesFrom(String resourceName) {
        List<String> lines = rawLinesFrom(resourceName, Function.identity());
        return lines.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .filter(li -> !li.startsWith("#"));
    }

    public static <T> T rawLinesFrom(String resourceName, Function<List<String>, T> xformer) {

        CharSource charSource = findResource(resourceName);
        try {
            List<String> lines = charSource.readLines();
            return xformer.apply(lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CharSource findResource(String resourceName) {
        try {
            return Files.asCharSource(DataFiles.load(resourceName), Charsets.UTF_8);
        } catch (MissingResourceException e1) {
            // see if its just a full file
            File file = new File(resourceName);
            if (file.exists()) {
                return Files.asCharSource(file, Charsets.UTF_8);
            }
            try {
                URL classpathResource = Resources.getResource(resourceName);
                return Resources.asCharSource(classpathResource, Charsets.UTF_8);
            } catch (MissingResourceException | IllegalArgumentException e) {
                throw new MissingResourceException("Tried to find " + resourceName +
                        " using data files and classpath; cannot find it", e
                );
            }
        }
    }

    public static ByteSource findByteResource(String resourceName) {
        try {
            URL classpathResource = Resources.getResource(resourceName);
            return Resources.asByteSource(classpathResource);
        } catch (IllegalArgumentException e) {
            // resource wasn't found on the classpath; try going through data files
            try {
                return Files.asByteSource(DataFiles.load(resourceName));
            } catch (Exception e1) {
                // see if its just a full file
                File file = new File(resourceName);
                if (file.exists()) {
                    return Files.asByteSource(file);
                }
                throw e1;
            }
        }
    }

    /**
     * Loads a "count table" from the resource path; a count table has a value and a count
     * @param resourceName
     * @return
     */
    public static CsvTable loadCountTable(String resourceName) {
        return CsvTable.loadSource(ReadWrite.findResource(resourceName))
                .hasHeaders()
                .autoDetectSeparator()
                .trimResults()
                .resolveMismatchWith((vals, headerCount) -> ImmutableList.of(
                        SPACE_JOINER.join(vals.subList(0, vals.size() - 1)),
                        vals.get(vals.size() - 1)
                ))
                .build();
    }

    /**
     * Loads a count table from the resource path, normalizes all of the entries and returns an
     * unnormalized multinomial distribution
     * @param resourceName
     * @param normalizer
     * @return
     */
    public static MutableMultinomial<String> loadCountTableAsMultinomial(String resourceName,
            Function<String, String> normalizer
    ) {
        CsvTable table = loadCountTable(resourceName);
        MutableMultinomial<String> multi = new MutableMultinomial<>(-1);
        for (Row row : table) {
            multi.add(normalizer.apply(row.getString(0)), row.getInt(1));
        }
        return multi;
    }

    public static void writeCountTable(Multinomial<?> multinomial, File output, String delim) {
        writeCountTable(multinomial.rankedList(), output, delim);
    }

    public static void writeCountTable(Iterable<? extends Entry<?>> entries, File output, String delim) {
        try (PrintWriter pw = new PrintWriter(output)) {
            pw.println("value" + delim + "count");
            for (Entry<?> entry : entries) {
                pw.println(entry.getKey() + delim + (long) entry.getDoubleValue());
            }
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeRankTable(Iterable<? extends Entry<?>> entries, File output, String delim) {
        try (PrintWriter pw = new PrintWriter(output)) {
            pw.println("value" + delim + "count");
            int i = 0;
            for (Entry<?> entry : entries) {
                i += 1;
                pw.println(Integer.toString(i) + delim + (long) entry.getDoubleValue());
            }
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
}
