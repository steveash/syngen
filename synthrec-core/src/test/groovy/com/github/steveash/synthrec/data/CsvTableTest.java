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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable.Row;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
public class CsvTableTest {
    private static final Logger log = LoggerFactory.getLogger(CsvTableTest.class);

    @Test
    public void shouldReadWithHeaders() throws Exception {

        int count = 0;
        ArrayList<String> lasts = Lists.newArrayList();
        for (Row row : CsvTable.loadResource("file-with-header.txt").build()) {
            lasts.add(row.getString("Last"));
            count += 1;
        }
        assertEquals(3, count);
        assertEquals(ImmutableList.of("Ash","Bash","Zee"), lasts);
    }

    @Test
    public void shouldReadNoHeaders() throws Exception {

        int count = 0;
        ArrayList<String> lasts = Lists.newArrayList();
        for (Row row : CsvTable.loadResource("file-no-header.txt").noHeaders(3).build()) {
            lasts.add(row.getString(1));
            count += 1;
        }
        assertEquals(3, count);
        assertEquals(ImmutableList.of("Ash","Bash","Zee"), lasts);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenNoHeaders() throws Exception {
        CsvTable table = CsvTable.loadResource("file-no-header.txt").noHeaders(3).build();
        Iterator<Row> iterator = table.iterator();
        String last = iterator.next().getString("last");
        fail();
    }
}