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

package com.github.steveash.synthrec.gen;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Steve Ash
 */
public class OverridingGenAssignmentTest {

    @Test
    public void shouldFlatten() throws Exception {
        MutableGenAssignment base = new MutableGenAssignment();
        OverridingGenAssignment middle = new OverridingGenAssignment(base);
        OverridingGenAssignment top = new OverridingGenAssignment(middle);

        base.put("a", "A_BASE");
        base.put("b", "B_BASE");
        base.put("c", "C_BASE");
        base.put("d", "D_BASE");
        base.put("e", "E_BASE");

        middle.put("b", "B_MIDDLE");
        middle.put("c", "C_MIDDLE");
        middle.put("f", "F_MIDDLE");
        middle.put("g", "G_MIDDLE");

        top.put("g", "G_TOP");
        top.put("e", "E_TOP");
        top.put("c", "C_TOP");
        top.put("i", "I_TOP");

        assertEquals("A_BASE", top.get("a"));
        assertEquals("B_MIDDLE", top.get("b"));
        assertEquals("C_TOP", top.get("c"));
        assertEquals("D_BASE", top.get("d"));
        assertEquals("E_TOP", top.get("e"));
        assertEquals("F_MIDDLE", top.get("f"));
        assertEquals("G_TOP", top.get("g"));
        assertEquals("I_TOP", top.get("i"));

        MutableGenAssignment result = OverridingGenAssignment.flatten(top);

        assertEquals("A_BASE", result.get("a"));
        assertEquals("B_MIDDLE", result.get("b"));
        assertEquals("C_TOP", result.get("c"));
        assertEquals("D_BASE", result.get("d"));
        assertEquals("E_TOP", result.get("e"));
        assertEquals("F_MIDDLE", result.get("f"));
        assertEquals("G_TOP", result.get("g"));
        assertEquals("I_TOP", result.get("i"));
    }
}