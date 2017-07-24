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

import static org.junit.Assert.*;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Ash
 */
public class DirichletSamplerTest {
    private static final Logger log = LoggerFactory.getLogger(DirichletSamplerTest.class);

    @Ignore
    @Test
    public void shouldSample() throws Exception {
        SummaryStatistics stats = new SummaryStatistics();
        GammaDistribution dist = new GammaDistribution(10000.0, 1.0);
        for (int i = 0; i < 100; i++) {
            double val = dist.sample();
            log.info("gamma smaple 100 = " + val);
            stats.addValue(val);
        }
        log.info("Stats " + stats);
    }
}