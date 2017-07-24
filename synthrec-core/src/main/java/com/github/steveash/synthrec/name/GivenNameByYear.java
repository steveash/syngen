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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.DataFiles;
import com.github.steveash.synthrec.ssa.SurvivalProb;
import com.github.steveash.synthrec.util.MoreMath;
import com.google.common.base.Stopwatch;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * The SSA given name file with frequency counts shaded by survival rates against the basis year
 * the counts are absolute so you may want to normalize or scale them depending on your needs (i.e. for virtual
 * counts for a prior you may want to scale)
 * @author Steve Ash
 */
public class GivenNameByYear {
    private static final Logger log = LoggerFactory.getLogger(GivenNameByYear.class);

    private static final int STARTING_YEAR = 1880;
    private static final int ENDING_YEAR = 2015; // min/max years we have data for
    private static final double MIN_PROB = 0.0001;

    public static GivenNameByYear makeWithBasis(int basisYear) {
        Stopwatch watch = Stopwatch.createStarted();
        SurvivalProb sprob = SurvivalProb.makeWithBasis(basisYear);
        Object2DoubleOpenHashMap<String> males = new Object2DoubleOpenHashMap<>(100_000);
        Object2DoubleOpenHashMap<String> females = new Object2DoubleOpenHashMap<>(100_000);

        int starting = Math.min(basisYear, STARTING_YEAR);
        int ending = Math.min(basisYear, ENDING_YEAR);
        for (int i = starting; i <= ending; i++) {
            int thisAge = Math.max(basisYear - i, 0);
            double prob = sprob.probOfSurvivalToAge(thisAge);
            prob = MoreMath.minMax(prob, MIN_PROB, 1.0);
            fillFile(males, females, prob, i);
        }
        GivenNameByYear names = new GivenNameByYear(males, females);
        watch.stop();
        log.info("Loaded SSA name frequencies in " + watch);
        return names;
    }

    private static void fillFile(Object2DoubleOpenHashMap<String> males,
            Object2DoubleOpenHashMap<String> females,
            double prob,
            int year
    ) {
        CsvTable table = CsvTable.loadFile(DataFiles.load("names/ssa/yob" + year + ".clob"))
                .noHeaders(3)
                .trimResults()
                .build();
        for (Row row : table) {
            String gender = row.getString(1);
            String name = Names.normalizeIntern(row.getString(0));
            int count = row.getInt(2);
            double scaled = count * prob;
            if (gender.equalsIgnoreCase("f")) {
                females.addTo(name, scaled);
            } else if (gender.equalsIgnoreCase("m")) {
                males.addTo(name, scaled);
            }
        }
    }

    private final Object2DoubleOpenHashMap<String> males;
    private final Object2DoubleOpenHashMap<String> females;
    private final double sum;
    private final double min;

    private GivenNameByYear(Object2DoubleOpenHashMap<String> males, Object2DoubleOpenHashMap<String> females) {
        this.males = males;
        this.females = females;
        double sum = 0;
        double min = Double.POSITIVE_INFINITY;
        for (Entry<String> entry : males.object2DoubleEntrySet()) {
            sum += entry.getDoubleValue();
            min = Math.min(min, entry.getDoubleValue());
        }
        for (Entry<String> entry : females.object2DoubleEntrySet()) {
            sum += entry.getDoubleValue();
            min = Math.min(min, entry.getDoubleValue());
        }
        this.sum = sum;
        this.min = min;
    }

    public double countMale(String normalName) {
        return males.getDouble(normalName);
    }

    public double countFemale(String normalName) {
        return females.getDouble(normalName);
    }

    public double countAll(String normalName) {
        return countMale(normalName) + countFemale(normalName);
    }

    public double normalizationConstant() {
        return sum;
    }

    public double minimumEntry() {
        return min;
    }

    public int sizeMale() {
        return males.size();
    }

    public int sizeFemale() {
        return females.size();
    }

    public void printStats() {
        SummaryStatistics maleStats = new SummaryStatistics();
        SummaryStatistics femaleStats = new SummaryStatistics();
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Entry<String> entry : males.object2DoubleEntrySet()) {
            maleStats.addValue(entry.getDoubleValue());
            stats.addValue(entry.getDoubleValue());
        }
        for (Entry<String> entry : females.object2DoubleEntrySet()) {
            femaleStats.addValue(entry.getDoubleValue());
            stats.addValue(entry.getDoubleValue());
        }
        log.info("Male stats >>>>\n" + maleStats.toString());
        log.info("Female stats >>>>\n" + femaleStats.toString());
        log.info("Min entry: " + min);
        for (int i = 0; i < 10; i++) {
            double ii = 0.1 + (i * 0.1);
            log.info("Percentile " + ii + " = " + stats.getPercentile(ii));
        }
        for (int i = 1; i < 10; i += 1) {
            log.info("Percentile " + i + " = " + stats.getPercentile(i));
        }
        for (int i = 10; i < 100; i += 10) {
            log.info("Percentile " + i + " = " + stats.getPercentile(i));
        }
        MinMaxPriorityQueue<Double> bottom50 = MinMaxPriorityQueue.maximumSize(500).create();
        for (double value : stats.getValues()) {
            bottom50.add(value);
        }
        int i = 0;
        for (Double value : Ordering.natural().sortedCopy(bottom50)) {
            log.info("Min value " + i + " = " + value);
            i += 1;
        }
    }

    public SetView<String> distinctNames() {
        return Sets.union(males.keySet(), females.keySet());
    }

    public boolean contains(String normalName) {
        return males.containsKey(normalName) || females.containsKey(normalName);
    }
}
