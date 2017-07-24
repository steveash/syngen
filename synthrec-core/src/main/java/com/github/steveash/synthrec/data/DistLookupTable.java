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

import static kylmshade.a.a.e.h;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * A lookup table to lookup a distribution (as a Distribution) for a given categorical value
 * where its expected that this categorical value is large and loaded from a CSV counts file
 * such as label,countA,countB,countC, etc.
 *
 * Not really a compact representation so consider that
 *
 * @author Steve Ash
 */
public class DistLookupTable {

    public static Multinomial<String> marginalizeCsv(CsvTable csv, Function<String, String> normalizer) {
        MutableMultinomial<String> output = new MutableMultinomial<>(0);
        for (Row row : csv) {
            int fieldCount = csv.getHeaders().size();
            long count = 0;
            for (int i = 1; i < fieldCount; i++) {
                count += row.getInt(i);
            }
            String rowValue = normalizer.apply(row.getString(0));
            if (isNotBlank(rowValue) && count > 0) {
                output.add(rowValue, count);
            }
        }
        return output;
    }

    private final Function<String,String> normalize;
    private final ImmutableSet<String> distCategories;
    private final ImmutableMap<String, Multinomial<String>> dists;

    public DistLookupTable(CsvTable csv) {this(csv, Function.identity());}

    public DistLookupTable(CsvTable csv,
            Function<String, String> normalizer
    ) {
        Builder<String, Multinomial<String>> builder = ImmutableMap.builder();
        List<String> headers = csv.getHeaders().stream().map(normalizer).collect(Collectors.toList());
        int fieldCount = headers.size();
        for (Row row : csv) {
            MutableMultinomial<String> dens = new MutableMultinomial<>(fieldCount - 1);
            for (int i = 1; i < fieldCount; i++) {
                int val = row.getInt(i);
                if (val > 0) {
                    dens.add(headers.get(i), val);
                }
            }
            builder.put(normalizer.apply(row.getString(0)), dens.normalize().toImmutable());
        }
        this.normalize = normalizer;
        this.dists = builder.build();
        this.distCategories = ImmutableSet.copyOf(headers.subList(1, headers.size()));
    }

    @Nullable
    public Multinomial<String> lookup(String normalizedLabel) {
        return dists.get(normalizedLabel);
    }

    @Nullable
    public Multinomial<String> lookupFromRaw(String rawToNormalizeAndLookup) {
        return lookup(normalize.apply(rawToNormalizeAndLookup));
    }

    public ImmutableSet<String> categories() {
        return distCategories;
    }
}
