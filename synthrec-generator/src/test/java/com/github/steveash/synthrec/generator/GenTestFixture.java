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

package com.github.steveash.synthrec.generator;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.steveash.synthrec.generator.GenTestFixture.TestBeans;

/**
 * @author Steve Ash
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SynthrecApplication.class, TestBeans.class}, properties = {
        "spring.jmx.enabled=false"
})
@ActiveProfiles({GenTestFixture.TEST,DictBeans.DICTS})
public class GenTestFixture {

    public static final String TEST = "unittest";

    @Configuration
    public static class TestBeans {

        @Bean
        public RandomGenerator randomGenerator() {
            return new Well19937c(0x415534BC);
        }
    }
}
