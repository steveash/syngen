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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.name.Gender;

/**
 * @author Steve Ash
 */
public class ConfusionTest {
    private static final Logger log = LoggerFactory.getLogger(ConfusionTest.class);

    @Test
    public void shouldPrintTable() throws Exception {
        Confusion<Gender> confusion = Confusion.makeForOutcomes(Gender.Male, Gender.Female, Gender.Both);
        confusion.add(Gender.Male, Gender.Male, 123);
        confusion.add(Gender.Female, Gender.Female, 456);
        confusion.add(Gender.Male, Gender.Female, 12);
        confusion.add(Gender.Female, Gender.Male, 23);
        log.info("\n" + confusion.toTableString());
    }
}