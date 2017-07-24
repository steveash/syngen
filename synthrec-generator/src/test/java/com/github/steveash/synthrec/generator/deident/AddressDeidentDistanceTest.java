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

package com.github.steveash.synthrec.generator.deident;

import javax.annotation.Resource;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.generator.GenTestFixture;

/**
 * @author Steve Ash
 */
public class AddressDeidentDistanceTest extends GenTestFixture {
    private static final Logger log = LoggerFactory.getLogger(AddressDeidentDistanceTest.class);

    @Resource private AddressDeidentDistance addressDeidentDistance;

    @Test
    public void shouldSmokeTestLoad() throws Exception {
        log.info("Loaded " + addressDeidentDistance.allPublicTokens().size() + " tokens");
    }
}