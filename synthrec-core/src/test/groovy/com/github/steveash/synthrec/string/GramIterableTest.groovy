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

package com.github.steveash.synthrec.string

/**
 * @author Steve Ash
 */
class GramIterableTest extends GroovyTestCase {

    void testGrams() {
        assert [].toSet() == GramIterable.grams("", 3).toSet()
        assert [].toSet() == GramIterable.grams("AB", 3).toSet()
        assert ["AB"].toSet() == GramIterable.gramsOrDefault("AB", 3).toSet()
        assert ["STE", "TEV", "EVE"].toSet() == GramIterable.grams("STEVE", 3).toSet()
    }
}
