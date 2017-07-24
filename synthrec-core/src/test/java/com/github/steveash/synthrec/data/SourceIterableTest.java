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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Closer;
import com.google.common.io.Resources;

/**
 * @author Steve Ash
 */
public class SourceIterableTest {

    @Test
    public void shouldIterateMany() throws Exception {
        try (Closer closer = Closer.create()) {
            File file1 = new File(Resources.getResource("file-no-header.txt").toURI());
            File file2 = new File(Resources.getResource("file-with-header.txt").toURI());
            Iterable<String> iterable = SourceIterable.fromMany(closer, Charsets.UTF_8, file1, file2);
            Iterator<String> iter = iterable.iterator();
            for (int i = 0; i < 7; i++) {
                assertTrue(iter.hasNext());
                if (i == 0) {
                    assertEquals("Steve,Ash,20", iter.next());
                } else {
                    assertTrue(isNotBlank(iter.next()));
                }
            }
            assertFalse(iter.hasNext());
        }
    }
}