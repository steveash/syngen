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

package com.github.steveash.synthrec.generator.profiling.explore;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import com.github.steveash.synthrec.stat.Multinomial;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * @author Steve Ash
 */
public class HistoCompare {

    public static <T> void writeChart(File outFile, Multinomial<T> first, Multinomial<T> second) {
        Preconditions.checkArgument(first.isNormalized());
        Preconditions.checkArgument(second.isNormalized());
        DefaultCategoryDataset dd = new DefaultCategoryDataset();
        for (Entry<T> entry : first.entries()) {
            dd.addValue(entry.getDoubleValue(), "first", entry.getKey().toString());
        }
        for (Entry<T> entry : second.entries()) {
            dd.addValue(entry.getDoubleValue(), "second", entry.getKey().toString());
        }
        JFreeChart chart = ChartFactory.createBarChart("Categorical Compare", "distribution", "value", dd);
        BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setItemMargin(0.0);
        try {
            ChartUtilities.saveChartAsJPEG(outFile, chart, 1280, 1024);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
