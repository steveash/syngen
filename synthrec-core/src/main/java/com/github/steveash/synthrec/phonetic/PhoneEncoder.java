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

import java.util.Set;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableSet;

/**
 * Contract for all phonetic encoders
 * @author Steve Ash
 */
public interface PhoneEncoder {

    String encode(String normalInput);

    default Set<String> encodeAllVariations(String normalInput) {
        return ImmutableSet.of(encode(normalInput));
    }

    default Set<String> encodeAllGreaterThan(String normalInput, int minSize) {
        return this.encodeAllVariations(normalInput).stream()
                .filter(key -> key.length() >= minSize)
                .collect(Guavate.toImmutableSet());
    }
}
