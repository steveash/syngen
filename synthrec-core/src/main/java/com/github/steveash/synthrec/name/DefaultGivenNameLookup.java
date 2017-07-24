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

package com.github.steveash.synthrec.name;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.ReadWrite;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;

/**
 * The default just uses the given names from the census data + SSA baby names + DMF names; you are encouraged to
 * build your own load method that adds your proprietary name dictionaries
 * @author Steve Ash
 */
public class DefaultGivenNameLookup implements GivenNameLookup {
    private static final Logger log = LoggerFactory.getLogger(DefaultGivenNameLookup.class);

    public static GivenNameLookup load() {
        log.info("Loading DefaultGivenNameLookup...");
        Stopwatch watch = Stopwatch.createStarted();
        GivenNameByYear ssaNames = GivenNameByYear.makeWithBasis(2015);
        CensusGivenNames census = CensusGivenNames.loadCensusData();
        CsvTable dmfs = ReadWrite.loadCountTable("names/dmf/name.GivenName.freq.clob");

        ImmutableSet<String> allNames = Streams.stream(
                Iterables.concat(ssaNames.distinctNames(),
                        census.maleNames(),
                        census.femaleNames(),
                        dmfs.columnsAsStringIter(0)
                ))
                .map(Names::normalizeIntern)
                .collect(ImmutableSet.toImmutableSet());
        watch.stop();
        log.info("...completed load of " + allNames.size() + " in " + watch.toString());
        return new DefaultGivenNameLookup(allNames);
    }

    private final ImmutableSet<String> allNames;

    public DefaultGivenNameLookup(ImmutableSet<String> allNames) {
        this.allNames = allNames;
    }

    @Override
    public Iterable<String> allNames() {
        return allNames;
    }

    @Override
    public boolean isPublicName(String normalName) {
        return allNames.contains(normalName);
    }
}
