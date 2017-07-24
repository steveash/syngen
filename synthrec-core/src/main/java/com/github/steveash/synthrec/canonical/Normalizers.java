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

package com.github.steveash.synthrec.canonical;

import com.google.common.base.CharMatcher;
import com.google.common.base.Functions;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * Constructor methods for common situations; most normalizers are written XtoY where X is the expected state of the
 * input string and Y is the output state. "raw" means completely raw incoming string
 * @author Steve Ash
 */
public class Normalizers {

    private static final Interner<String> strings = Interners.newStrongInterner();

    public static StringNormalizer rawToStandard() {
        return GeneralNormalizer.INSTANCE;
    }

    public static StringNormalizer standardToName() {
        return NameNormalizer.INSTANCE;
    }

    public static StringNormalizer internAsNormalizer() {
        return strings::intern;
    }

    public static Interner<String> interner() {
        return strings;
    }

    public static StringNormalizer onlyDigits() {
        return CharMatcher.digit()::retainFrom;
    }
}
