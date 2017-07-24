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

import static com.google.common.truth.Truth.assertThat;

import org.apache.commons.math3.random.Well19937c;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.stat.Dists.ConditionalBuilder;

/**
 * Smoother of conditional distributions for sampling with backoff
 * @author Steve Ash
 */
public class BackoffSmootherTest {
    private static final Logger log = LoggerFactory.getLogger(BackoffSmootherTest.class);

    @Test
    public void shouldMarginal() throws Exception {
        BackoffSmoother<String> smoother = BackoffSmoother
                .<String>startingWith(1.0, 1.0, "FN", "LN")
                .nextOn("FN")
                .build();

        ConditionalBuilder<String> builder = Dists.condBuilder();
        builder.adderFor("FN", "steve", "LN", "ash")
                .add("a", 10.0)
                .add("b", 5.0)
                .add("c", 3.0)
                .add("d", 1.0);
        builder.adderFor("FN", "steve", "LN", "jones")
                .add("a", 5.0)
                .add("b", 5.0)
                .add("c", 5.0)
                .add("d", 5.0);
        builder.adderFor("FN", "nancy", "LN", "jones")
                .add("a", 3.0)
                .add("b", 5.0)
                .add("c", 10.0);
        smoother.smoothFirstPass(builder.getConditional());

        AssignmentInstance fnlnSteveAsh = builder.assignFor("FN", "steve", "LN", "ash");
        AssignmentInstance fnSteve = builder.assignFor("FN", "steve");

        // lets look at the unsmoothed vs smoothed
        log.info("Unsmoothed " + smoother.levelDists.get(0).get(fnlnSteveAsh).toString());
        log.info("Smoothed " + smoother.outputs.get(0).get(fnlnSteveAsh).toString());

        assertThat(smoother.levelDists).hasSize(3);
        assertThat(smoother.outputs).hasSize(2);

        ConditionalSampler<String> sampler = smoother.smoothSampler(builder.getConditional());
        AssignmentInstance fnTammy = AssignmentInstance.make("FN", "tammy", "LN", "baker");
        Well19937c rand = new Well19937c(0xCAFE1234);

        MutableMultinomial<String> empiSteve = MutableMultinomial.createUnknownMax();
        MutableMultinomial<String> empiTammy = MutableMultinomial.createUnknownMax();
        for (int i = 0; i < 1000; i++) {
            empiSteve.add(sampler.sample(rand, fnlnSteveAsh), 1.0);
            empiTammy.add(sampler.sample(rand, fnTammy), 1.0);
        }
        log.info("SteveSamples " + empiSteve.toString());
        log.info("TammySamples " + empiTammy.toString());
        // the tammy one is getting the final backoff which has C and A very close whereas the "steve" dist is skewed to A
    }
}