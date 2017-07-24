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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Simple algorithm for smoothing an empirical distribution given a prior that fits the prior to
 * the empirical first (using virtual count that minimizes SSE) and then applying a factor to
 * control the overall power
 * @author Steve Ash
 */
public class EmpPriorSmoother {
    private static final Logger log = LoggerFactory.getLogger(EmpPriorSmoother.class);

    private boolean isDumpOut = false;
    private double alpha = 0.05;
    private double minVirtualCount = 5000;
    private boolean onlyEmitCommonEntries = false;

    public EmpPriorSmoother(double alpha, double minVirtualCount) {this(alpha, minVirtualCount, false);}

    public EmpPriorSmoother(
            double alpha,
            double minVirtualCount,
            boolean onlyEmitCommonEntries
    ) {
        this.alpha = alpha;
        this.minVirtualCount = minVirtualCount;
        this.onlyEmitCommonEntries = onlyEmitCommonEntries;
    }

    public boolean isDumpOut() {
        return isDumpOut;
    }

    public void setDumpOut(boolean dumpOut) {
        this.isDumpOut = dumpOut;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getMinVirtualCount() {
        return minVirtualCount;
    }

    public void setMinVirtualCount(double minVirtualCount) {
        this.minVirtualCount = minVirtualCount;
    }

    public boolean isOnlyEmitCommonEntries() {
        return onlyEmitCommonEntries;
    }

    public void setOnlyEmitCommonEntries(boolean onlyEmitCommonEntries) {
        this.onlyEmitCommonEntries = onlyEmitCommonEntries;
    }

    /**
     * Scale the prior to fit it to the magnitude of the empirical and then add the empirical
     * to it
     * @param empirical
     * @param priorToModify the prior that will be mutated (scaled) and then the empirical is added to it
     * @param tag just a label to use in messages and in the dump file
     * @param <T>
     */
    public <T> void smoothPriorCopy(Multinomial<T> empirical, MutableMultinomial<T> priorToModify, String tag) {
        double newVirtCount = calcVirtualCount(empirical, priorToModify, tag, true);
        priorToModify.scaleToVirtualCount(newVirtCount);
        if (this.isDumpOut) {
            dumpOut(empirical, priorToModify, tag);
        }
        if (onlyEmitCommonEntries) {
            removeEntriesNotIn(priorToModify, empirical, tag);
        }
        priorToModify.addMultinomial(empirical);
    }

    private <T> void removeEntriesNotIn(MutableMultinomial<T> priorToModify, Multinomial<T> empirical, String tag) {
        ObjectIterator<Entry<T>> iter = priorToModify.entries().fastIterator();
        int removedCount = 0;
        while (iter.hasNext()) {
            Entry<T> next = iter.next();
            if (!empirical.contains(next.getKey())) {
                iter.remove();
                removedCount += 1;
            }
        }
        log.info("For " + tag + " removed " + removedCount + " entries from smoothed prior");
    }

    public <T> double calcVirtualCount(Multinomial<T> empirical, Multinomial<T> prior, String tag) {
        return calcVirtualCount(empirical, prior, tag, false);
    }

    public <T> double calcVirtualCount(Multinomial<T> empirical, Multinomial<T> prior, String tag, boolean shouldLog) {
        double allEmpirical = empirical.sum();
        double allPrior = prior.sum();
        // these are sums of the matching entries
        double empSum = 0;
        double priSum = 0;

        ObjectIterator<Entry<T>> iter = empirical.entries().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            double empAmt = entry.getDoubleValue();
            double priAmt = prior.get(entry.getKey());
            empSum += (empAmt * priAmt);
            priSum += (priAmt * priAmt);
        }
        double newVirtCount;
        if (priSum > 0) {
            newVirtCount = Math.max(minVirtualCount, allPrior * (empSum / priSum) * alpha);
        } else {
            // use the top entry
            double empMax = empirical.summaryStatsOverCounts().getMax();
            double priMax = prior.summaryStatsOverCounts().getMax();
            Preconditions.checkState(priMax > 0, "empty prior");
            newVirtCount = Math.max(minVirtualCount, allPrior * (empMax / priMax) * alpha);
        }
        if (shouldLog) {
            log.info("For " + tag + " using virt count " + newVirtCount + " emp sum " + allEmpirical +
                    " used max? " + (priSum <= 0));
        }
        return newVirtCount;
    }

    private void dumpOut(Multinomial<?> empirical, Multinomial<?> priorMulti, String tag) {
        File outf = new File("smoothing." + tag + ".csv");
        try (PrintWriter pw = new PrintWriter(Files.newWriter(outf, Charsets.UTF_8))) {
            pw.println("value,empirical,prior,smoothed");
            for (Object2DoubleMap.Entry<?> entry : empirical.rankedList()) {
                double ev = entry.getDoubleValue();
                double pv = priorMulti.get(entry.getKey());
                pw.println(entry.getKey() + "," + ev + "," + pv + "," + (ev + pv));
            }
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
}
