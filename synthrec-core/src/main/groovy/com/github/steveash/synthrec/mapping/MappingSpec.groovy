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

package com.github.steveash.synthrec.mapping

import com.github.steveash.synthrec.data.InvalidDataException
import com.github.steveash.synthrec.domain.Record
import com.google.common.base.Function
import com.google.common.base.Functions
import com.google.common.base.Preconditions
import groovy.transform.CompileStatic

import static org.apache.commons.lang3.StringUtils.isNotBlank


/**
 * @author Steve Ash
 */
class MappingSpec {

    List<MappingAction> actions = []

    void mapFrom(String headerFrom, String valueTo) {
        actions << new FromToAction(headerFrom, valueTo, Functions.identity())
    }

    void mapFrom(String headerFrom, String valueTo, Closure<String> xform) {
        actions << new FromToAction(headerFrom, valueTo, xform as Function)
    }

    void map(@DelegatesTo(MappingDelegate) Closure block) {
        actions << new ClosureMappingAction(block.dehydrate())
    }

    @CompileStatic
    static class ClosureMappingAction implements MappingAction {

        private final Closure block;

        ClosureMappingAction(Closure blk) {
            this.block = blk
        }

        @Override
        void execute(Map<String, Integer> headerToIndex, List<String> fields, Record sink) {
            def delegate = new MappingDelegate(headerToIndex, fields, sink)
            def blk = block.rehydrate(delegate, delegate, delegate)
            try {
                blk()
            } catch (Exception e) {
                throw new InvalidDataException("Problem running mapping block on " + fields, e)
            }
        }
    }

    @CompileStatic
    static class MappingDelegate {
        private final Map<String, Integer> headerToIndex
        private final List<String> fields
        private final Record sink

        MappingDelegate(Map<String, Integer> headerToIndex, List<String> fields, Record sink) {
            this.headerToIndex = headerToIndex
            this.fields = fields
            this.sink = sink
        }

        String read(String fieldName) {
            def index = headerToIndex.getOrDefault(fieldName, -1)
            Preconditions.checkArgument(index >= 0, "bad index for field ", fieldName)
            Preconditions.checkArgument(index < fields.size(), "too big index for ", fieldName)
            return fields.get(index)
        }

        void write(String field, CharSequence value) {
            sink.setInitialValue(field.toString(), value.toString())
        }

        String readAsCsv(List<String> fieldNames) {
            StringBuilder sb = new StringBuilder()
            boolean hasValues = false;
            for (String fieldName : fieldNames) {
                def value = read(fieldName)
                if (isNotBlank(value)) {
                    if (hasValues) {
                        sb.append(", ")
                    }
                    sb.append(value)
                    hasValues = true
                }
            }
            return sb.toString()
        }

        String readAsCsv(String... fieldNames) {
            return readAsCsv(Arrays.asList(fieldNames))
        }
    }
}
