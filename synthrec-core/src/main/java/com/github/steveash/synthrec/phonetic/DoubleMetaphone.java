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

package com.github.steveash.synthrec.phonetic;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
public class DoubleMetaphone implements PhoneEncoder {

    public static final int DEFAULT_DBL_LEN = 8;
    public static final DoubleMetaphone INSTANCE = new DoubleMetaphone(DEFAULT_DBL_LEN);

    private final int encodingLength;

    public DoubleMetaphone(int encodingLength) {
        this.encodingLength = encodingLength;
    }

    @Override
    public String encode(String normalInput) {
        org.apache.commons.codec.language.DoubleMetaphone metaphone = makeEncoder();
        String first = metaphone.doubleMetaphone(normalInput, false);
        return first;
    }

    private org.apache.commons.codec.language.DoubleMetaphone makeEncoder() {
        org.apache.commons.codec.language.DoubleMetaphone metaphone = new org.apache.commons.codec.language.DoubleMetaphone();
        metaphone.setMaxCodeLen(encodingLength);
        return metaphone;
    }

    @Override
    public Set<String> encodeAllVariations(String normalInput) {
        org.apache.commons.codec.language.DoubleMetaphone metaphone = makeEncoder();
        String first = metaphone.doubleMetaphone(normalInput, false);
        String second = metaphone.doubleMetaphone(normalInput, true);
        boolean firstBlank = isBlank(first);
        boolean secondBlank = isBlank(second) || (!firstBlank && first.equals(second));
        if (firstBlank && secondBlank) {
            return ImmutableSet.of();
        }
        if (!firstBlank && secondBlank) {
            return ImmutableSet.of(first);
        }
        if (firstBlank && !secondBlank) {
            return ImmutableSet.of(second);
        }
        return ImmutableSet.of(first, second);
    }


}
