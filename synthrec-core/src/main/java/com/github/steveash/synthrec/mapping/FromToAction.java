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

package com.github.steveash.synthrec.mapping;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;

import com.github.steveash.synthrec.domain.Record;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * @author Steve Ash
 */
public class FromToAction implements MappingAction {

    private final String fromField;
    private final String toKey;
    private final Function<String,String> xform;

    public FromToAction(String fromField, String toKey, Function<String, String> xform) {
        this.fromField = fromField;
        this.toKey = toKey;
        this.xform = xform;
    }

    @Override
    public void execute(Map<String, Integer> headerToIndex, List<String> fields, Record sink) {
        int index = headerToIndex.getOrDefault(fromField, -1);
        Preconditions.checkArgument(index >= 0, "No header value ", fromField);
        Preconditions.checkArgument(index < fields.size(), "Index bigger than fields", index, fields);
        String maybe = fields.get(index);
        if (isNotBlank(maybe)) {
            sink.setInitialValue(toKey, xform.apply(maybe));
        }
    }
}
