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
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Ash
 */
public class ConcurrentHistogramTest {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentHistogramTest.class);

    @Test
    public void shouldSimpleTest() throws Exception {
        ConcurrentHistogram histo = new ConcurrentHistogram(0, 100.0, 20);
        histo.add(15.0);
        histo.add(15.0);
        histo.add(25.0);
        histo.add(67.0);
        histo.add(99.0);
        log.info(histo.toString());
        assertThat(histo.getCountAt(17)).isEqualTo(2);
    }
}