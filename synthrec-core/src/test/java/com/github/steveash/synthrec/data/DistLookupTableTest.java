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

package com.github.steveash.synthrec.data;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.stat.Multinomial;

/**
 * @author Steve Ash
 */
public class DistLookupTableTest {
    private static final Logger log = LoggerFactory.getLogger(DistLookupTableTest.class);

    @Test
    public void shouldLookup() throws Exception {
        DistLookupTable table = new DistLookupTable(CsvTable.loadResource("jointsample.csv").build());
        Multinomial<String> stevea = table.lookup("STEVEA");
        Multinomial<String> stevee = table.lookup("STEVEE");
        Multinomial<String> stevei = table.lookup("STEVEI");
        log.info("STEVEA " + stevea.toString());
        assertEquals("A", stevea.best());
        assertEquals("B", stevee.best());
        assertEquals("C", stevei.best());
        assertNull(table.lookup("DOESNTEXIST"));
    }
}