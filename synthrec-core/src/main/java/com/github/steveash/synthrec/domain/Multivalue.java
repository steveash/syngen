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

package com.github.steveash.synthrec.domain;

import java.util.Iterator;

import com.google.common.collect.ImmutableList;

/**
 * When a feature needs to encode multiple values as co-occurring states in an assignment then the feature
 * needs to return an instance of multivalue which contains the bag of values.
 *
 * this should never be serialized. if it does get serialized that means some edge isn't handling the multiplicity
 * @author Steve Ash
 */
public class Multivalue<T> implements Iterable<T> {

    private final ImmutableList<T> valueBag;

    public Multivalue(Iterable<T> valueBag) {this.valueBag = ImmutableList.copyOf(valueBag);}

    public Multivalue(T... valueBag) { this.valueBag = ImmutableList.copyOf(valueBag); }

    @Override
    public Iterator<T> iterator() {
        return valueBag.iterator();
    }

    public boolean hasMany() {
        return valueBag.size() > 1;
    }

    public int size() {
        return valueBag.size();
    }

    public ImmutableList<T> getValueBag() {
        return valueBag;
    }

    @Override
    public String toString() {
        return "Multivalue{" +
                "valueBag=" + valueBag +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Multivalue objects = (Multivalue) o;

        return valueBag.equals(objects.valueBag);
    }

    @Override
    public int hashCode() {
        return valueBag.hashCode();
    }
}
