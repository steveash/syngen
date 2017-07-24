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

package com.github.steveash.synthrec.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Ash
 */
public class MoreMathTest {
    private static final Logger log = LoggerFactory.getLogger(MoreMathTest.class);

    @Test
    public void shouldUseLogs() throws Exception {
        double a = Math.log(0.091235345423432);
        double b = Math.log(0.131235876532234);
        double more = MoreMath.sumLogProb(a, b);
        double normal = Math.log(Math.exp(a) + Math.exp(b));
        log.info("Got \n" + more + "\n" + normal);
        assertEquals(more, normal, 0.000000000001);
        more = Math.exp(more);
        normal = Math.exp(normal);
        log.info("Got \n" + more + "\n" + normal);
        assertEquals(more, normal, 0.000000000001);
    }
}