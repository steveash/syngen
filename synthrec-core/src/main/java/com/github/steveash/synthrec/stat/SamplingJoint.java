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

package com.github.steveash.synthrec.stat;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * A class that represents a categorical frequency joint distribution of P(X) that allows categorical
 * values (X) to be sampled weighted by their total frequency _or_ conditioned by Y P(X|Y) and then
 * sample the conditional distribution of X.  It is assumed that |X| >> |Y| (e.g. X is names, Y is race)
 * the values here are cumulative (negative log(prob)) (for precision)
 * @author Steve Ash
 */
public class SamplingJoint {

    private final String[] xValues;                     // X
    private final Object2IntMap<String> yValues;        // Y -> index into condtionalCumLogProb
    private final List<double[]> condCumLogProb; // P(X|Y)

    /**
     * Create the sampling table from entries containing raw counts of occurrence
     * @param table
     */
    public SamplingJoint(CsvTable table) {
        int yCount = table.getHeaders().size() - 1;
        Object2IntOpenHashMap<String> yVals = new Object2IntOpenHashMap<>(yCount);
        yVals.defaultReturnValue(-1);
        // the first col is X so skip it
        for (int i = 1; i < table.getHeaders().size(); i++) {
            yVals.put(normal(table.getHeaders().get(i)), i - 1);
        }
        this.yValues = yVals;
        long[] ySum = new long[yCount];
        int count = 0;
        for (Row row : table) {
            count += 1;
            for (int i = 1; i <= yCount; i++) {
                ySum[i - 1] += row.getLong(i);
            }
        }
        long[] cum = new long[yCount];
        int i = 0;
        this.xValues = new String[count];
        this.condCumLogProb = Lists.newArrayListWithCapacity(yCount);
        for (int j = 0; j < yCount; j++) {
            condCumLogProb.add(new double[count]);
        }

        double[] denom = new double[yCount];
        for (int j = 0; j < yCount; j++) {
            denom[j] = Math.log(ySum[j]);
        }
        for (Row row : table) {
            xValues[i] = row.getString(0);
            for (int j = 0; j < yCount; j++) {
                cum[j] += row.getLong(j + 1);
                condCumLogProb.get(j)[i] = Math.log(cum[j]) - denom[j];
            }
            i += 1;
        }
    }

    private String normal(String s) {
        return s.toLowerCase();
    }

    public String sampleUniform(RandomGenerator rand) {
        return xValues[rand.nextInt(xValues.length)];
    }

    public String sampleWeighted(RandomGenerator rand, String conditionedOn) {
        int yIndex = this.yValues.getInt(normal(conditionedOn));
        if (yIndex < 0) {
            throw new IllegalArgumentException("Cannot condition on unknown value " + conditionedOn +
                    "; only can condition on " + this.yValues.keySet());
        }
        double[] cumLogProb = condCumLogProb.get(yIndex);
        double nextValue = Math.log(rand.nextDouble());
        int found = Arrays.binarySearch(cumLogProb, nextValue);
        if (found >= 0) {
            // by magic we hit the exact value
            return xValues[found];
        }
        // binary search returns (-insertPlace) - 1 so recover insert spot, thats our sample
        int insert = -(found + 1);
        Preconditions.checkState(insert >= 0 && insert <= xValues.length);
        if (insert >= xValues.length) {
            return xValues[xValues.length - 1];
        }
        return xValues[insert];
    }
}
