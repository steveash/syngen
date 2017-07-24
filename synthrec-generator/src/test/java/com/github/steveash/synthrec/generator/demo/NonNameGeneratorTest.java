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

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.generator.GenTestFixture;

/**
 * @author Steve Ash
 */
public class NonNameGeneratorTest extends GenTestFixture {
    private static final Logger log = LoggerFactory.getLogger(NonNameGeneratorTest.class);

    @Resource private NonNameGenerator nonNameGenerator;
    @Resource private RandomGenerator rand;

    @Test
    public void shouldGenerateSamples() throws Exception {
        for (int i = 0; i < 10; i++) {
            int size = 1 + (i % 3);
            List<String> genn = nonNameGenerator.phrase(rand, size);
            log.info("Got " + genn);
            assertThat(genn).hasSize(size);
        }
    }
}