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

package com.github.steveash.synthrec.ssa;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Steve Ash
 */
public class SurvivalProbTest {

    @Test
    public void shouldSurvive() throws Exception {
        SurvivalProb yr2000 = SurvivalProb.makeWithBasis(2000);
        SurvivalProb yr2010 = SurvivalProb.makeWithBasis(2010);
        SurvivalProb yr2050 = SurvivalProb.makeWithBasis(2050);
        assertEquals(1.0, yr2000.probOfSurvivalToAge(0), 0.001);
        assertEquals(1.0, yr2010.probOfSurvivalToAge(0), 0.001);
        assertEquals(1.0, yr2050.probOfSurvivalToAge(0), 0.001);
        assertEquals(0.17, yr2000.probOfSurvivalToAge(90), 0.01);
        assertEquals(0.19, yr2010.probOfSurvivalToAge(90), 0.01);
        assertEquals(0.28, yr2050.probOfSurvivalToAge(90), 0.01);
        assertEquals(0.0, yr2050.probOfSurvivalToAge(150), 0.001);
    }
}