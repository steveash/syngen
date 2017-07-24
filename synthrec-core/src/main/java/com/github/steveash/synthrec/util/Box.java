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

package com.github.steveash.synthrec.util;

/**
 * Holder for pass by ref
 * @author Steve Ash
 */
public class Box<T> {

    private T ref;

    public Box(T ref) {
        this.ref = ref;
    }

    public Box() {
    }

    public T get() {
        return ref;
    }

    public void set(T ref) {
        this.ref = ref;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Box<?> box = (Box<?>) o;

        return ref != null ? ref.equals(box.ref) : box.ref == null;
    }

    @Override
    public int hashCode() {
        return ref != null ? ref.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Box{" +
                "ref=" + ref +
                '}';
    }
}
