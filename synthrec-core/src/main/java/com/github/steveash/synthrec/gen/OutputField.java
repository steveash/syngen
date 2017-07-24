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

import javax.annotation.CheckReturnValue;

import com.google.common.base.Function;

/**
 * Represents a single output field
 * @author Steve Ash
 */
public class OutputField {

    @CheckReturnValue
    public static OutputField create(String genAssignKey) {
        return new OutputFieldBuilder().setGenAssignKey(genAssignKey).create();
    }

    @CheckReturnValue
    public static OutputFieldBuilder builder(String genAssignKey) {
        return new OutputFieldBuilder().setGenAssignKey(genAssignKey);
    }

    private final Function<Object,String> formatter;
    private final String header;
    private final String genAssignKey;
    private final String defaultValue;

    OutputField(Function<Object, String> formatter, String header, String genAssignKey, String defaultValue) {
        this.formatter = formatter;
        this.header = header;
        this.genAssignKey = genAssignKey;
        this.defaultValue = defaultValue;
    }

    public String render(GenAssignment assign) {
        Object value = assign.tryGet(genAssignKey);
        String result = defaultValue;
        if (value != null) {
            result = formatter.apply(value);
        }
        if (result == null) {
            throw new IllegalArgumentException("Trying to output value " + value + " for gen key " + genAssignKey +
                    " but can't output null values; " + assign);
        }
        return result;
    }

    public String getHeader() {
        return header;
    }

    public String getGenAssignKey() {
        return genAssignKey;
    }
}
