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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.steveash.synthrec.generator.spring.LazyComponent;

/**
 * @author Steve Ash
 */
@LazyComponent
public class InputFileFactory {

    private final MapperFactory defaultMapper;
    private final InputConfig inputConfig;

    @Autowired
    public InputFileFactory(MapperFactory mapperFactory, InputConfig inputConfig) {
        this.defaultMapper = mapperFactory;
        this.inputConfig = inputConfig;
    }

    public InputFile makeDefault() {
        return new InputFile(defaultMapper, inputConfig);
    }

    public InputFile make(InputConfig thisConfig) {
        return new InputFile(defaultMapper, thisConfig);
    }
}
