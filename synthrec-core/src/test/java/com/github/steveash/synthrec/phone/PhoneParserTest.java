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

package com.github.steveash.synthrec.phone;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.steveash.synthrec.phone.PhoneParser.PhoneModel;

/**
 * @author Steve Ash
 */
public class PhoneParserTest {

    private final PhoneParser parser = new PhoneParser();

    @Test
    public void shouldParseEasy() throws Exception {
        assertParse("+1-901-299-3573", "+1 901 299 3573");
        assertParse("901-299-3573", "901-299-3573");
        assertParse("901-299-3573", "(901) 299-3573");
        assertParse("901-299-3573", "(901)299-3573");
        assertParse("901-299-3573", "(901)2993573");
        assertParse("901-299-3573", " 901.299.3573  ");
        assertParse("901-299-3573", "9012993573");
        assertParse("+1-901-299-3573", " +1901.299.3573  ");
        assertParse("+1-901-299-3573", " 19012993573  ");
    }

    @Test
    public void shouldParseNoArea() throws Exception {
        assertParse("000-299-3573", "2993573");
        assertParse("000-299-3573", "299-3573");
        assertParse("000-299-3573", "299 3573 ");
        assertParse("000-299-3573", "299.3573 ");
    }

    private void assertParse(String expected, String input) {
        PhoneModel model = parser.parse(input);
        assertNotNull("Cant parse " + input, model);
        assertEquals(expected, model.toCanonicalString());
    }
}