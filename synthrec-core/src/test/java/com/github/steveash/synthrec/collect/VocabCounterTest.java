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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * @author Steve Ash
 */
public class VocabCounterTest {

    private Vocabulary<String> vocab;
    private VocabCounter<String> counter;

    @Before
    public void setUp() throws Exception {
        vocab = new Vocabulary<>();
        counter = new VocabCounter<>(vocab);
    }

    @Test
    public void shouldIterate() throws Exception {
        Map<String, Double> map = ImmutableMap.of("steve", 123.0, "ash", 456.0, "bob", 789.0);
        map.entrySet().forEach(e -> counter.addByValue(e.getKey(), e.getValue()));

        HashMap<String,Double> outmap = Maps.newHashMap();

        Iterator<Entry<String>> iter = counter.fastIterator();
        while (iter.hasNext()) {
            Entry<String> entry = iter.next();
            outmap.put(entry.getKey(), entry.getDoubleValue());
        }
        assertEquals(map, outmap);
    }
}