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

package com.github.steveash.synthrec.collect.kdtree;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Steve Ash
 */
public class KDTreeTest {

    @Test
    public void shouldSmokeTest() throws Exception {
        KDTree kdt = new KDTree(2);
        kdt.insert(new double[] {1.0, 1.0}, "A");
        kdt.insert(new double[] {0.1, 0.1}, "B");
        kdt.insert(new double[] {0.2, 0.2}, "C");
        kdt.insert(new double[] {0.4, 0.4}, "D");
        kdt.insert(new double[] {0.4, 0.45}, "E");
        kdt.insert(new double[] {0.4, 0.46}, "F");

        assertEquals("D", kdt.nearest(new double[]{0.4, 0.4}));
        Object[] nearest = kdt.nearest(new double[]{0.4, 0.4}, 4);
        assertEquals(4, nearest.length);
        assertEquals("D", nearest[0]);
        assertEquals("E", nearest[1]);
        assertEquals("F", nearest[2]);
        assertEquals("C", nearest[3]);

        Object[] nearest2 = kdt.nearest(new double[]{0.401, 0.401}, 4);
        assertEquals(4, nearest2.length);
        assertEquals("D", nearest2[0]);
        assertEquals("E", nearest2[1]);
        assertEquals("F", nearest2[2]);
        assertEquals("C", nearest2[3]);
    }
}