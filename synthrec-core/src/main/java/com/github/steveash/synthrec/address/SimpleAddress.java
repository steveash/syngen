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

package com.github.steveash.synthrec.address;

import java.util.Iterator;
import java.util.List;

/**
 * Simplest impl of an Address
 * @author Steve Ash
 */
public class SimpleAddress implements Address {

    private final List<AddressSegment> segs;

    public SimpleAddress(List<AddressSegment> segs) {this.segs = segs;}

    @Override
    public AddressSegment get(int index) {
        return segs.get(index);
    }

    @Override
    public int size() {
        return segs.size();
    }

    @Override
    public boolean isEmpty() {
        return segs.isEmpty();
    }

    @Override
    public Iterator<AddressSegment> iterator() {
        return segs.iterator();
    }
}
