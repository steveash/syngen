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

package com.github.steveash.synthrec.string;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;

/**
 * @author Steve Ash
 */
public class GramIterable {

    public static Iterable<String> gramsOrDefault(String value, int gramSize) {
        if (isBlank(value)) {
            return ImmutableList.of();
        }
        if (value.length() < gramSize) {
            return ImmutableList.of(value);
        }
        return grams(value, gramSize);
    }

    public static Iterable<String> grams(String value, int gramSize) {
        if (isBlank(value)) {
            return ImmutableList.of();
        }
        return () -> new AbstractIterator<String>() {
            private int i = 0;

            @Override
            protected String computeNext() {
                if (i + gramSize <= value.length()) {
                    String gram = value.substring(i, i + gramSize);
                    i += 1;
                    return gram;
                }
                return endOfData();
            }
        };
    }
}
