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

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Ash
 */
public class DeathProbTest {
    private static final Logger log = LoggerFactory.getLogger(DeathProbTest.class);

    @Test
    public void shouldLookup() throws Exception {
        DeathProb deathProb = DeathProb.makeDefault();
        for (int i = 0; i < 140; i += 20) {
            double mm = deathProb.deathProbAtAge(2015, i, true);
            double ff = deathProb.deathProbAtAge(2015, i, false);

            log.info("Male " + i + " = " + mm);
            log.info("Female " + i + " = " + ff);
        }
        assertEquals(1.0, deathProb.deathProbAtAge(2015, 150, true), 0.001);
        assertEquals(1.0, deathProb.deathProbAtAge(2015, 150, false), 0.001);
        assertEquals(0.0067, deathProb.deathProbAtAge(2015, 0, true), 0.0001);
        assertEquals(0.0056, deathProb.deathProbAtAge(2015, 0, false), 0.0001);
    }
}