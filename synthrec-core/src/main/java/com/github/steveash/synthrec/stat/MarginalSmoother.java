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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * A generic smoother for conditional distributions that calculates the marginal distribution from the conditional
 * and then scales that to the number of virtual counts that you specify (or based on a scaled percentile
 * of the observed virtual counts (which is arbitrary but maybe better than a random guess??)
 * @author Steve Ash
 */
@Deprecated // use the BackoffSmoother instead which usesthe EmpPrior smoothing strategy (and supports backoff)
public class MarginalSmoother {
    private static final Logger log = LoggerFactory.getLogger(MarginalSmoother.class);

    private final double percentileToUseForVirtualCount;
    private final double scaleFactorForPercentileValue;
    private final double percentOfEmpirical;

    public MarginalSmoother(double percentileToUseForVirtualCount, double scaleFactorForPercentileValue) {
        this.percentileToUseForVirtualCount = percentileToUseForVirtualCount;
        this.scaleFactorForPercentileValue = scaleFactorForPercentileValue;
        this.percentOfEmpirical = -1;
    }

    public MarginalSmoother(double percOfEmpiricalCount) {
        this.percentileToUseForVirtualCount = -1;
        this.scaleFactorForPercentileValue = -1;
        this.percentOfEmpirical = percOfEmpiricalCount;
    }

    /**
     * @param name
     * @param conditionalCopy
     * @param factorParents
     * @return
     */
    public <T> BackoffSampler<T> smoothByMarginal(String name,
            Map<AssignmentInstance, MutableMultinomial<T>> conditionalCopy,
            Set<String> factorParents
    ) {
        if (percentileToUseForVirtualCount >= 0) {
            return smoothByPercentiles(name, conditionalCopy, factorParents);
        }
        return smoothByVirtualCount(name, conditionalCopy, factorParents);
    }

    private <T> BackoffSampler<T> smoothByVirtualCount(String name,
            Map<AssignmentInstance, MutableMultinomial<T>> conditionalCopy,
            Set<String> factorParents
    ) {
        Preconditions.checkState(this.percentOfEmpirical >= 0);
        MutableMultinomial<T> marginal = new MutableMultinomial<>(-1);
        double sum = 0;
        for (MutableMultinomial<T> multi : conditionalCopy.values()) {
            sum += multi.sum();
            marginal.addMultinomial(multi);
        }
        double virtualCount = sum * this.percentOfEmpirical;
        marginal.scaleToVirtualCount(virtualCount);
        log.info("For " + name + " scaling to " + virtualCount);
        HashMap<AssignmentInstance, SamplingTable<T>> map = Maps.newHashMap();
        for (Entry<AssignmentInstance, MutableMultinomial<T>> entry : conditionalCopy.entrySet()) {
            MutableMultinomial<T> multi = entry.getValue();
            multi.addMultinomial(marginal);
            map.put(entry.getKey(), SamplingTable.createFromMultinomial(multi));
        }
        return new BackoffSampler<>(map,
                ConditionalSampler.adaptSampler(SamplingTable.createFromMultinomial(marginal)),
                factorParents
        );
    }

    private <T> BackoffSampler<T> smoothByPercentiles(String name,
            Map<AssignmentInstance, MutableMultinomial<T>> conditionalCopy,
            Set<String> factorParents
    ) {
        MutableMultinomial<T> marginal = new MutableMultinomial<>(-1);
        // we're going to take the 33rd percentile of the sums and take half of that and use that to scale;
        // this is an arbitrary heuristic that I should validate
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Multinomial<T> cond : conditionalCopy.values()) {
            marginal.addMultinomial(cond);
            stats.addValue(cond.summaryStatsOverCounts().getSum());
        }
        log.info("For " + name + " there are " + conditionalCopy.size() + " conditional distributions and the " +
                "1/25/33/50/75/99 percentiles of counts are " + stats.getPercentile(1.0) +
                ", " + stats.getPercentile(25.0) +
                ", " + stats.getPercentile(33.0) +
                ", " + stats.getPercentile(50.0) +
                ", " + stats.getPercentile(75.0) +
                ", " + stats.getPercentile(99.0) + " and stats are " + stats);

        double virtualCount = stats.getPercentile(percentileToUseForVirtualCount);
        virtualCount *= scaleFactorForPercentileValue;
        log.info("For " + name + " scaling to " + virtualCount);
        marginal.scaleToVirtualCount(virtualCount);

        HashMap<AssignmentInstance, SamplingTable<T>> map = Maps.newHashMap();
        for (Entry<AssignmentInstance, MutableMultinomial<T>> entry : conditionalCopy.entrySet()) {
            MutableMultinomial<T> multi = (MutableMultinomial) entry.getValue();
            multi.addMultinomial(marginal);
            map.put(entry.getKey(), SamplingTable.createFromMultinomial(multi));
        }
        return new BackoffSampler<>(map,
                ConditionalSampler.adaptSampler(SamplingTable.createFromMultinomial(marginal)),
                factorParents
        );
    }
}
