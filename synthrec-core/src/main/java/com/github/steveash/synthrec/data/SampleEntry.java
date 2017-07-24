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

import it.unimi.dsi.fastutil.objects.AbstractObject2DoubleMap.BasicEntry;

/**
 * Just an easy way to get to a fastutil Object2Double entry which i use a lot
 * @author Steve Ash
 */
public class SampleEntry<T> extends BasicEntry<T> {

    public SampleEntry(T key, Double value) {
        super(key, value);
    }

    public SampleEntry(T key, double value) {
        super(key, value);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
