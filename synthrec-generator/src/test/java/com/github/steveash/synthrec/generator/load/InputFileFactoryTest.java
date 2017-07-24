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

package com.github.steveash.synthrec.generator.load;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.generator.GenTestFixture;
import com.google.common.collect.Iterables;

/**
 * @author Steve Ash
 */
public class InputFileFactoryTest extends GenTestFixture {

    @Resource private InputFileFactory inputFileFactory;

    @Test
    public void shouldLoadInputFile() throws Exception {
        InputFile inputFile = inputFileFactory.makeDefault();
        assertEquals(50, Iterables.size(inputFile));
    }
}