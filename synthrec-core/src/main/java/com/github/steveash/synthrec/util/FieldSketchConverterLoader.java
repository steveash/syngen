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

package com.github.steveash.synthrec.util;

import org.apache.commons.convert.AbstractConverter;
import org.apache.commons.convert.ConversionException;
import org.apache.commons.convert.ConverterLoader;
import org.apache.commons.convert.Converters;
import org.apache.commons.convert.GenericSingletonToList;
import org.apache.commons.convert.GenericSingletonToSet;

import com.github.steveash.synthrec.domain.FieldSketch;

/**
 * @author Steve Ash
 */
public class FieldSketchConverterLoader implements ConverterLoader {

    public static class FieldSketchToString extends AbstractConverter<FieldSketch, String> {

        public FieldSketchToString() {
            super(FieldSketch.class, String.class);
        }

        @Override
        public String convert(FieldSketch obj) throws ConversionException {
            return obj.toSketchOnlyString();
        }
    }

    @Override
    public void loadConverters() {
        Converters.loadContainedConverters(FieldSketchConverterLoader.class);
        Converters.registerConverter(new GenericSingletonToList<>(FieldSketch.class));
        Converters.registerConverter(new GenericSingletonToSet<>(FieldSketch.class));
    }
}
