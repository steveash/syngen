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

import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;

import java.util.Iterator;
import java.util.concurrent.atomic.LongAdder;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;

/**
 * @author Steve Ash
 */
public class ConcurrentHistogram {

    /**
     * Represents one bin in the histogram; the count is the count in the bin
     */
    @Immutable
    public static class HistogramBin {
        public final double binMinimumInclusive;
        public final double binMaximumExclusive;
        public final int binIndex;

        public final long count;

        private HistogramBin(double binMinimumInclusive, double binMaximumExclusive, int binIndex, long count) {
            this.binMinimumInclusive = binMinimumInclusive;
            this.binMaximumExclusive = binMaximumExclusive;
            this.binIndex = binIndex;
            this.count = count;
        }

        @Override
        public String toString() {
            return String.format("[%.3f,%.3f)=%d", binMinimumInclusive, binMaximumExclusive, count);
        }
    }

    private static Predicate<HistogramBin> EmptyBinFilter = input -> input.count > 0;

    private static final Function<HistogramBin, String> convertBinToString = HistogramBin::toString;

    private final double min;
    private final double maxExcl;
    private final int binCount;
    private final double binWidth;
    private final LongAdder[] histogram;

    public ConcurrentHistogram(double min, double maxExcl, int binCount) {
        this.min = min;
        this.maxExcl = maxExcl;
        this.binCount = binCount;
        this.binWidth = computeBinWidth(min, maxExcl, binCount);
        this.histogram = new LongAdder[binCount];
        for (int i = 0; i < binCount; i++) {
            this.histogram[i] = new LongAdder();
        }
    }

    private double computeBinWidth(double min, double maxExcl, int binCount) {
        return (maxExcl - min) / binCount;
    }

    /**
     * Returns the count in this bin where the bin is identified by the 0-based index;
     * i.e. if there are 10 bins for x values [0.0, 1.0) then the x bin at index 0 covers
     * the range [0.0, 0.10) and the x bin at index 9 (the last bin) covers the range
     * [0.9, 1.0)
     */
    public long getCountAtIndex(int index) {
        return histogram[index].sum();
    }

    public String getRangeLabelAtIndex(int index) {
        double rangeMin = (index * binWidth) + min;
        return String.format("[%.2f,%.2f)", rangeMin, rangeMin + binWidth);
    }

    /**
     * Returns the count in this bin which corresponds to where the given value
     * would be placed. Thus, if you had a histogram covering [0.0, 1.0) with 10 bins,
     * then asking for the count at 0.15 would return the count of the bin corresponding
     * to index 1, because the 1st index corresponds to the interval [0.1, 0.2).
     */
    public long getCountAt(double sampleValue) {
        int index = convertSampleToBinIndex(sampleValue);
        return getCountAtIndex(index);
    }

    /**
     * Adds one sample to the histogram (i.e. increments the count in the bucket corresponding to x)
     */
    public void add(double x) {
        int index = convertSampleToBinIndex(x);
        histogram[index].increment();
    }

    public void add(double x, int deltaToAdd) {
        int index = convertSampleToBinIndex(x);
        histogram[index].add(deltaToAdd);
    }

    public int convertSampleToBinIndex(double sampleValue) {
        if (sampleValue <= min)
            return 0;

        double shiftedToZero = (sampleValue - min);
        int index = (int) Math.floor(shiftedToZero / binWidth);

        if (index >= binCount)
            return binCount - 1;

        return index;
    }

    @VisibleForTesting
    double getMinBinRange(int index) {
        return index * binWidth + min;
    }

    @VisibleForTesting
    double getMaxBinRange(int index) {
        return getMinBinRange(index) + binWidth;
    }

    public Iterator<HistogramBin> iterator() {
        return new AbstractIterator<HistogramBin>() {
            int cursor = 0;

            @Override
            protected HistogramBin computeNext() {
                if (cursor >= histogram.length)
                    return endOfData();

                HistogramBin ret = new HistogramBin(
                        getMinBinRange(cursor),
                        getMaxBinRange(cursor),
                        cursor,
                        histogram[cursor].sum()
                );
                cursor++;
                return ret;
            }
        };
    }

    public Iterator<HistogramBin> iteratorNonEmptyBins() {
        return filter(iterator(), EmptyBinFilter);
    }

    public String nonEmptyBinsAsString() {
        return Joiner.on(',').join(transform(iteratorNonEmptyBins(), convertBinToString));
    }

    public String nonEmptyBinsAsStringLines() {
        return Joiner.on('\n').join(transform(iteratorNonEmptyBins(), convertBinToString));
    }

    @Override
    public String toString() {
        return "Histogram [histo=" + nonEmptyBinsAsString() + "]";
    }

    public double getMin() {
        return min;
    }

    public double getMaxExcl() {
        return maxExcl;
    }

    public int getBinCount() {
        return binCount;
    }

    public double getBinWidth() {
        return binWidth;
    }

    public void clear() {
        for (int i = 0; i < this.histogram.length; i++) {
            this.histogram[i].reset();
        }
    }
}
