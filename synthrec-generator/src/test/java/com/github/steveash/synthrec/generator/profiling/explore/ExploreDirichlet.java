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
import java.util.Random;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.stat.DirichletSampler;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.RateLimiter;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * @author Steve Ash
 */
public class ExploreDirichlet {
    private static final Logger log = LoggerFactory.getLogger(ExploreDirichlet.class);

    public static void main(String[] args) {
        CsvTable table = CsvTable.loadResource("namecultdist2.txt").hasHeaders().trimResults().build();
        MutableMultinomial<String> first = new MutableMultinomial<>(table.estimateRowCount().get());
        table.forEach(e -> first.add(e.getString("Culture"), e.getInt("Count")));

        RandomGenerator rand = new JDKRandomGenerator(0xCAE21223);
        Multinomial<String> multinomial = first.toImmutable();
        first.normalize();

        MutableMultinomial<String> addSmooth = MutableMultinomial.copyFrom(multinomial);
        addSmooth.addToAllPresent(0.5);

        // sample to build the second distribution; then we'll measure the difference between them
        SamplingTable<String> smoothSampling = SamplingTable.createFromNormalized(addSmooth.normalize());
        DirichletSampler<String> dirichlet = DirichletSampler.fromMultinomialAndHyperPrior(multinomial, 0.5);
        MutableMultinomial<String> second = new MutableMultinomial<>(first.maxEntries());
        MutableMultinomial<String> third = new MutableMultinomial<>(first.maxEntries());
        RateLimiter limiter = RateLimiter.create(1.0);
        SamplingTable<String> sampleDist = null;
        for (int i = 0; i < 10_000; i++) {
            if (i % 100 == 0) {
                sampleDist = dirichlet.sampleTable();
            }
            String maybe = sampleDist.sampleWeighted(rand);
            Preconditions.checkNotNull(maybe);
            second.add(maybe, 1.0);
            third.add(smoothSampling.sampleWeighted(rand), 1.0);
            if (i % 64 == 0) {
                if (limiter.tryAcquire()) {
                    log.info("Completed iter " + i);
                }
            }
        }
        second.normalize();
        third.normalize();

        compare(first, second, "empirical-to-dirichlet");
        compare(first, third, "empirical-to-addsmooth");
        compare(second, third, "dirichlet-to-addsmooth");
    }

    public static void compare(MutableMultinomial<String> first, MutableMultinomial<String> second, String label) {
        System.out.println();
        System.out.println("**************************************************************************");
        System.out.println(">> " + label);
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Entry<String> entry : first.entries()) {
            double fe = entry.getDoubleValue();
            double se = second.get(entry.getKey());
            stats.addValue(Math.abs(fe - se));
        }
        System.out.println("KL divergence = " + first.kullbackLieblerTo(second));
        System.out.println("JensonShannon = " + first.jensonShannonDivergence(second));
        System.out.println("Avg diff " + stats.getMean());
        System.out.println("Min diff " + stats.getMin());
        System.out.println("25p diff " + stats.getPercentile(25));
        System.out.println("50p diff " + stats.getPercentile(50));
        System.out.println("75p diff " + stats.getPercentile(75));
        System.out.println("Max diff " + stats.getMax());
        HistoCompare.writeChart(new File("sampling-explore-" + label + ".jpg"), first, second);
    }
}
