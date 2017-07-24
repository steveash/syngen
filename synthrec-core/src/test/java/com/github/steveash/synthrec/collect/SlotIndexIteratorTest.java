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

package com.github.steveash.synthrec.collect;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.truth.Truth.assertThat;

import java.util.Iterator;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * @author Steve Ash
 */
public class SlotIndexIteratorTest {

    @Test
    public void shouldIterEmpty() throws Exception {
        assertThat(SlotIndexIterator.makeFor(of()).hasNext()).isFalse();
        assertThat(SlotIndexIterator.makeFor(of(of("A"), of())).hasNext()).isFalse();
    }

    @Test
    public void shouldIterSingle() throws Exception {
        Iterator<IntArrayList> iter = SlotIndexIterator.makeFor(of(of("A")));
        IntArrayList list = iter.next();
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.getInt(0)).isEqualTo(0);
        assertThat(iter.hasNext()).isFalse();
    }

    @Test
    public void shouldIterMany() throws Exception {
        Iterator<IntArrayList> iter = SlotIndexIterator.makeFor(of(of("A", "B"), of("c", "d", "e")));
        IntArrayList row0 = iter.next();
        IntArrayList row1 = iter.next();
        IntArrayList row2 = iter.next();
        IntArrayList row3 = iter.next();
        IntArrayList row4 = iter.next();
        IntArrayList row5 = iter.next();
        assertThat(iter.hasNext()).isFalse();
        assertThat(row0.getInt(0)).isEqualTo(0);
        assertThat(row0.getInt(1)).isEqualTo(0);
        assertThat(row1.getInt(0)).isEqualTo(1);
        assertThat(row1.getInt(1)).isEqualTo(0);
        assertThat(row2.getInt(0)).isEqualTo(0);
        assertThat(row2.getInt(1)).isEqualTo(1);
        assertThat(row3.getInt(0)).isEqualTo(1);
        assertThat(row3.getInt(1)).isEqualTo(1);
        assertThat(row4.getInt(0)).isEqualTo(0);
        assertThat(row4.getInt(1)).isEqualTo(2);
        assertThat(row5.getInt(0)).isEqualTo(1);
        assertThat(row5.getInt(1)).isEqualTo(2);
    }
}