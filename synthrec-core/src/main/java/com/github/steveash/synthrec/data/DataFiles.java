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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Splitter;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Utilities for loading data files that should exist in the project
 * @author Steve Ash
 */
public class DataFiles {

    private static final Supplier<List<File>> rootSupplier = Suppliers.memoize(() -> {
        Splitter splitter = Splitter.on(File.pathSeparator);
        String root = System.getProperty("synth.data", null);
        if (isBlank(root)) {
            throw new IllegalStateException("No synth.data system property is specified; this needs to contain" +
                    "the list of paths where loadable data resides (like a PATH or CLASSPATH but for data)");
        }
        Builder<File> builder = ImmutableList.builder();
        for (String rootPath : splitter.split(root)) {
            File file = new File(rootPath);
            if (!file.exists()) {
                throw new IllegalStateException("synth.data contains the path [" + rootPath + "] but " +
                        "it does not exist");
            }
            builder.add(file);
        }
        return builder.build();
    });

    public static File load(String relativePath) {
        for (File root : rootSupplier.get()) {
            File result = new File(root, relativePath);
            if (result.exists()) {
                return result;
            }
        }
        throw new MissingResourceException("Data file to load doesnt exist: " + relativePath + " tried the synth.data " +
                "data path: " + rootSupplier.get());
    }
}
