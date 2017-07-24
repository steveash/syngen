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

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.Cacher;
import com.github.steveash.synthrec.data.ReadWrite;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * Wrapper around the census b's given name distributions
 * @author Steve Ash
 */
public class CensusGivenNames {
    private static final Logger log = LoggerFactory.getLogger(CensusGivenNames.class);

    private final Object2DoubleOpenHashMap<String> males;
    private final Object2DoubleOpenHashMap<String> females;

    public static CensusGivenNames loadCensusDataFromCache() {
        return Cacher.get(CensusGivenNames.class.getSimpleName(), CensusGivenNames::loadCensusData);
    }

    public static CensusGivenNames loadCensusData() {
        return new CensusGivenNames(load("names/census/census-male-firstnames.clob"),
                load("names/census/census-female-firstnames.clob"));
    }

    private static Object2DoubleOpenHashMap<String> load(String resourceName) {
        Stream<String> lines = ReadWrite.linesFrom(resourceName);
        Object2DoubleOpenHashMap<String> map = new Object2DoubleOpenHashMap<>();
        lines.forEach( line -> {
            String originalName = line.substring(0, 15).trim();
            String name = Names.normalizeIntern(originalName);
            double idf = Double.parseDouble(line.substring(15, 21).trim());
            map.put(name, idf);
        });
        return map;
    }

    private static String normalize(String input) {
        return Names.normalize(input);
    }

    public CensusGivenNames(Object2DoubleOpenHashMap<String> males, Object2DoubleOpenHashMap<String> females) {
        this.males = males;
        this.females = females;
    }

    /**
     * Returns < 0 if the name isn't present in the census data
     * @param candidate
     * @return
     */
    public double lookup(String candidate) {
        candidate = normalize(candidate);
        return lookupNormalized(candidate);
    }

    public Iterable<String> maleNames() {
        return males.keySet();
    }

    public Iterable<String> femaleNames() {
        return females.keySet();
    }

    public Iterable<String> distinctNames() {
        return Sets.union(males.keySet(), females.keySet());
    }

    public double lookupNormalized(String normalName) {
        double maybe = males.getDouble(normalName);
        if (maybe == males.defaultReturnValue()) {
            maybe = females.getDouble(normalName);
            if (maybe == females.defaultReturnValue()) {
                return -1.0;
            }
        }
        return maybe;
    }

    public boolean isInCensusNames(String normalName) {
        return lookupNormalized(normalName) > 0;
    }

    public double maleCountFor(String normalName) {
        return Math.max(0, males.getDouble(normalName));
    }

    public double femaleCountFor(String normalName) {
        return Math.max(0, females.getDouble(normalName));
    }

    public int maleCount() {
        return males.size();
    }

    public int femaleCount() {
        return females.size();
    }

    public double total() {
        double sum = 0;
        for (Object2DoubleMap.Entry<String> entry : males.object2DoubleEntrySet()) {
//            sum += Math.pow(2.0, entry.getDoubleValue());
            sum += (100000.0 / Math.exp(entry.getDoubleValue()));
        }
//        for (Object2DoubleMap.Entry<String> entry : females.object2DoubleEntrySet()) {
//            sum += Math.pow(10.0, entry.getDoubleValue());
//        }
        return sum;
    }

    @Override
    public String toString() {
        return "CensusGivenNames{MaleCount=" + males.size() + ", FemaleCount=" + females.size() + "}";
    }
}
