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

package com.github.steveash.synthrec.ssa;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import com.github.steveash.synthrec.ssa.DmfParser.DmfRecord;

/**
 * @author Steve Ash
 */
public class DmfParserTest {

    @Test
    public void shouldParseRecord() throws Exception {
        String line = " 185225219RICHARDS            SR  LEROY          G               1202199502151930";
        DmfRecord result = DmfParser.parse(line);
        assertThat(result.getGivenName()).isEqualTo("LEROY");
        assertThat(result.getMiddleName()).isEqualTo("G");
        assertThat(result.getFamilyName()).isEqualTo("RICHARDS");
        assertThat(result.getSuffix()).isEqualTo("SR");
        assertThat(result.getId()).isEqualTo("185225219");
        // i turned off the birthdate stuff for the moment since the DMF has some invalid dates
//        assertThat(result.getBirthDate().toString()).isEqualTo("1930-02-15");
//        assertThat(result.getDeathDate().toString()).isEqualTo("1995-12-02");

    }
}