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

/**
 * A single segment of an address - i.e. a "word" of an address.  This is really just a pojo container for the
 * text word, the tags, and some helper methods.
 *
 * The model will assign the most likely semantic tags.
 *
 * @author Steve Ash
 */
public class AddressSegment {

    public final String word;

    // hidden, result state
    public AddressTag semanticTag = null;

    public AddressSegment(String word) {
        this(word, null);
    }

    public AddressSegment(String word, AddressTag tag) {
        this.word = word;
        this.semanticTag = tag;
    }

    public AddressSegment copyWithDifferentWord(String word) {
        AddressSegment newSegment = new AddressSegment(word);
        newSegment.semanticTag = this.semanticTag;
        return newSegment;
    }

    @Override
    public String toString() {
        return "AddressSegment{" +
                "word='" + word + '\'' +
                ", semanticTag=" + semanticTag +
                '}';
    }
}
