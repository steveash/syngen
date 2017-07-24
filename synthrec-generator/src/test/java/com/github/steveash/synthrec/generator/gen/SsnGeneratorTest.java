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

package com.github.steveash.synthrec.generator.gen;

import javax.annotation.Resource;

import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.TranslationTable;
import com.github.steveash.synthrec.generator.GenTestFixture;
import com.github.steveash.synthrec.generator.demo.SsnGenerator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * @author Steve Ash
 */
public class SsnGeneratorTest extends GenTestFixture {
    private static final Logger log = LoggerFactory.getLogger(SsnGeneratorTest.class);

    @Resource SsnGenerator generator;
    @Resource private TranslationTable stateTranslationTable;
    private final RandomGenerator rand = new JDKRandomGenerator(0xAA12DE1);

    @Test
    public void shouldGenerateNew() throws Exception {
        ImmutableSet<String> states = stateTranslationTable.distinctTargetValues();
        for (int i = 0; i < 100; i++) {
            log.info("New " + generator.generate(rand, 2012, "TX"));
        }
        for (int i = 0; i < 100; i++) {
            String state = Iterables.get(states, rand.nextInt(states.size()));
            int birthYear = 2015 - rand.nextInt(100);
            log.info("Old " + state + " " + birthYear + " " + generator.generate(rand, birthYear, state));
        }
    }
}