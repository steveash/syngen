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

package com.github.steveash.synthrec.generator.demo;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.HashSet;

import javax.annotation.Resource;

import org.apache.commons.math3.random.Well19937c;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.generator.GenTestFixture;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class DobGeneratorTest extends GenTestFixture {
    private static final Logger log = LoggerFactory.getLogger(DobGeneratorTest.class);

    @Resource private DobGenerator dobGenerator;

    @Test
    public void shouldGenDobs() throws Exception {
        Well19937c rand = new Well19937c();
        HashSet<LocalDate> dates = Sets.newHashSet();
        for (int i = 0; i < 1000; i++) {

            LocalDate dob = dobGenerator.generate(rand, rand.nextInt(100));
            if (!dates.add(dob)) {
                log.info("Got duplicate dob " + dob);
            }
            if (i % 50 == 0) {
                log.info("Generated " + i + " and get " + dob);
            }
        }
        log.info("In that sample got " + dates.size() + " distinct");
        Assert.assertThat(dates.size(), Matchers.allOf(greaterThan(900), Matchers.lessThanOrEqualTo(1000)));
    }
}