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

package com.github.steveash.synthrec.socio;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;

/**
 * @author Steve Ash
 */
public class CsvZipDataLookup implements ZipDataLookup {
    private static final Logger log = LoggerFactory.getLogger(CsvZipDataLookup.class);

    public static ZipDataLookup loadFromDefault() {
        // loads from a sample CSV file that was compiled from the census b +
        // some github repos that had area codes and zip -> city name
        // this is NOT a good data set to use for high quality synth data because
        // census ZCTAs are NOT the same as USPS ZIP codes (what people report in
        // their demographics)
        return loadFromCsvResource("socio/census-zip-pop.clob");
    }

    public static ZipDataLookup loadFromCsvResource(String resourceName) {
        CsvTable table = CsvTable.loadResource(resourceName)
                .autoDetectSeparator()
                .hasHeaders()
                .trimResults()
                .build();
        ImmutableMap.Builder<String, ZipData> zips = ImmutableMap.builder();
        Stopwatch watch = Stopwatch.createStarted();
        int count = 0;
        for (Row row : table) {
            String zip = row.getString("zip");
            zips.put(zip, new SimpleZipData(
                    zip,
                    row.getInt("population"),
                    row.getString("city"),
                    row.getString("state"),
                    row.getString("cityid"),
                    row.getInt("citypop"),
                    row.getString("areacode")
            ));
            count += 1;
        }
        watch.stop();
        log.info("Loaded " + count + " zips from " + resourceName + " in " + watch);
        return new CsvZipDataLookup(zips.build());
    }

    private final ImmutableMap<String, ZipData> zips;

    private CsvZipDataLookup(ImmutableMap<String, ZipData> zips) {
        this.zips = zips;
    }

    @Override
    public Collection<ZipData> allZips() {
        return zips.values();
    }

    @Nonnull
    @Override
    public Optional<ZipData> apply(String zipCode) {
        return Optional.fromNullable(zips.get(zipCode));
    }
}
