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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.github.steveash.jg2p.util.Histogram;
import com.github.steveash.jg2p.util.Histogram.HistogramBin;
import com.github.steveash.jg2p.util.Percent;
import com.google.common.math.DoubleMath;

/**
 * @author Steve Ash
 */
public class HistogramPrint {

    private static final int LINELEN = 79;

    public static String printToString(Histogram h) {
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw);
        printHistoTo(printWriter, h);
        printWriter.flush();
        return sw.toString();
    }

    public static void printHistoTo(PrintWriter pw, Histogram h) {
        int maxLabelWidth = 0;
        long sum = 0;
        for (int i = 0; i < h.getBinCount(); i++) {
            sum += h.getCountAtIndex(i);
            maxLabelWidth = Math.max(maxLabelWidth, h.getRangeLabelAtIndex(i).length());
        }
        double entSum = 0;
        Iterator<HistogramBin> iter = h.iteratorNonEmptyBins();
        while (iter.hasNext()) {
            HistogramBin bucket = iter.next();
            double prob = Percent.value(bucket.count, sum);
            if (prob > 0) {
                entSum += (prob * DoubleMath.log2(prob));
            }
        }
        if (entSum < 0) {
            entSum = -entSum;
        }
        pw.println(StringUtils.center(String.format(" [%.4f, %.4f) w %.4f, entries %d ",
                h.getMin(),
                h.getMaxExcl(),
                h.getBinWidth(),
                sum
        ), LINELEN, '*'));
        double maxEnt = DoubleMath.log2(h.getBinCount());
        pw.println(StringUtils.center(String.format(" entropy %.4f, max entropy %.2f (%s) ",
                entSum,
                maxEnt,
                Percent.print(entSum, maxEnt)

        ), LINELEN, '*'));

        int pad = maxLabelWidth + 1 + 10;
        int grid = Math.max(0, LINELEN - pad);
        iter = h.iterator();
        while (iter.hasNext()) {
            HistogramBin bin = iter.next();
            double perc = Percent.value(bin.count, sum);
            int on = Math.min(grid, (int) (perc * ((double) grid)));
            int off = grid - on;
            String format = String.format("%s (%.4f) ", h.getRangeLabelAtIndex(bin.binIndex), perc);
            pw.println(
                    StringUtils.leftPad(format, pad) +
                            StringUtils.repeat('#', on) +
                            StringUtils.repeat('-', off)
            );
        }
        pw.println(StringUtils.repeat('*', LINELEN));
    }
}
