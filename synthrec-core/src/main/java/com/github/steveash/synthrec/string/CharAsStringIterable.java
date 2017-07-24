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

import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

/**
 * @author Steve Ash
 */
public class CharAsStringIterable implements Iterable<String> {

    private final String delegate;

    public CharAsStringIterable(String delegate) {this.delegate = delegate;}

    @Override
    public Iterator<String> iterator() {
        return new AbstractIterator<String>() {
            private int i = 0;

            @Override
            protected String computeNext() {
                if (i < delegate.length()) {
                    char next = delegate.charAt(i);
                    i += 1;
                    return Character.toString(next);
                }
                return endOfData();
            }
        };
    }
}
