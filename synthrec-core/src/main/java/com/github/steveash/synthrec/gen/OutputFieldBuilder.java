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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

public class OutputFieldBuilder {
    private Function<Object, String> formatter = Object::toString;
    private String header = null;
    private String genAssignKey = null;
    private String defaultValue = "";

    public OutputFieldBuilder setFormatter(Function<Object, String> formatter) {
        this.formatter = formatter;
        return this;
    }

    public OutputFieldBuilder setHeader(String header) {
        this.header = header;
        return this;
    }

    public OutputFieldBuilder setGenAssignKey(String genAssignKey) {
        this.genAssignKey = genAssignKey;
        if (header == null) {
            this.header = genAssignKey;
        }
        return this;
    }

    public OutputFieldBuilder setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public OutputField create() {
        Preconditions.checkNotNull(header);
        Preconditions.checkNotNull(genAssignKey);
        return new OutputField(formatter, header, genAssignKey, defaultValue);
    }
}