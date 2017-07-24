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

import org.junit.Test

import static com.github.steveash.synthrec.string.OptimalStringAlignment.editDistance

/**
 * @author Steve Ash
 */
class OptimalStringAlignmentTest {

    @Test
    public void shouldBeZeroForEqualStrings() throws Exception {
        assert 0 == editDistance("steve", "steve", 1)
        assert 0 == editDistance("steve", "steve", 0)
        assert 0 == editDistance("steve", "steve", 2)
        assert 0 == editDistance("steve", "steve", 100)

        assert 0 == editDistance("s", "s", 1)
        assert 0 == editDistance("s", "s", 0)
        assert 0 == editDistance("s", "s", 2)
        assert 0 == editDistance("s", "s", 100)

        assert 0 == editDistance("", "", 0)
        assert 0 == editDistance("", "", 1)
        assert 0 == editDistance("", "", 100)
    }

    @Test
    public void shouldBeOneForSingleOperation() throws Exception {
        def a = "steve";
        for (int i = 0; i < 5; i++) {
            assertOneOp(new StringBuilder(a).insert(i, 'f'), a)
            assertOneOp(new StringBuilder(a).deleteCharAt(i), a)
            def sb = new StringBuilder(a)
            sb.setCharAt(i, 'x' as char);
            assertOneOp(sb, a)

            if (i > 1) {
                sb = new StringBuilder(a)
                char t = sb.charAt(i - 1)
                sb.setCharAt(i - 1, sb.charAt(i))
                sb.setCharAt(i, t)
                println "comparing " + sb.toString() + " -> " + a
                assertOneOp(sb, a)
            }
        }
    }

    @Test
    public void shouldCountTransposeAsOne() throws Exception {
        assert 3 == editDistance("xxsteve", "steev", 4)
        assert 3 == editDistance("xxsteve", "steev", 3)
        assert 3 == editDistance("steev", "xxsteve", 4)
        assert 3 == editDistance("steev", "xxsteve", 3)
        assert -1 == editDistance("steev", "xxsteve", 2)

        assert 4 == editDistance("xxtseve", "steev", 4)
        assert 5 == editDistance("xxtsevezx", "steevxz", 5)
        assert 6 == editDistance("xxtsevezx", "steevxzpp", 6)
        assert 7 == editDistance("xxtsfevezx", "steevxzpp", 7)

        assert 4 == editDistance("xxtsf", "st", 7)
        assert 4 == editDistance("evezx", "eevxzpp", 7)
        assert 7 == editDistance("xxtsfevezx", "steevxzpp", 7)
    }

    @Test
    public void shouldCountLeadingCharacterTranspositionsAsOne() throws Exception {
        assert 1 == editDistance("rosa", "orsa", 2)
    }

    private void assertOneOp(CharSequence a, CharSequence b) {
        assert 1 == editDistance(a, b, 1)
        assert 1 == editDistance(b, a, 1)
        assert 1 == editDistance(a, b, 2)
        assert 1 == editDistance(b, a, 2)
    }

    @Test
    public void shouldShortCutWhenSpecialCase() throws Exception {
        assert 1 == editDistance("s", "", 1)
        assert 1 == editDistance("", "s", 1)
        assert -1 == editDistance("s", "", 0)
        assert -1 == editDistance("", "s", 0)
        assert -1 == editDistance("st", "", 1)
        assert -1 == editDistance("", "st", 1)
        assert -1 == editDistance("steve", "ste", 0)
        assert -1 == editDistance("ste", "steve", 0)
        assert -1 == editDistance("stev", "steve", 0)
        assert -1 == editDistance("ste", "steve", 1)
        assert -1 == editDistance("steve", "ste", 1)
        assert 1 == editDistance("steve", "stev", 1)
        assert 1 == editDistance("stev", "steve", 1)
    }
}
