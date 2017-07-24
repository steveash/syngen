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

package com.github.steveash.synthrec.domain;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.convert.Converters;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Sketch of a "field" (just any stream of tokens) is a list of _components_.  Each component is either a placeholder
 * or a literal. The sketch keeps track of which things are literals and which are placeholders.  A placeholder is
 * something that you use to represent the type of thing instead of the thing itself.  So a sketch of "MR STEPHEN M ASH"
 * might be MR GIVENNAME MIDDLEINITIAL FAMILYNAME in which case the first component is a literal and the rest are
 * placeholders
 * @author Steve Ash
 */
public class FieldSketch implements Serializable {

    private static final long serialVersionUID = -3229103039085000048L;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ObjectArrayList<String> sketch = new ObjectArrayList<>();
        private final ObjectArrayList<Object> literals = new ObjectArrayList<>();
        private final BitSet isLiteral = new BitSet();

        private Builder() {
        }

        public Builder addPlaceholder(@Nonnull String placeholderObj, @Nullable Object literalObj) {
            Preconditions.checkNotNull(placeholderObj, "cant add null through this api");
            addBoth(placeholderObj, literalObj);
            return this;
        }

        private void addBoth(@Nonnull String placeholderObj, @Nullable Object literalObj) {
//            if (literalObj != null) {
//                Preconditions.checkArgument(literalObj instanceof Serializable, "obj isnt serializable", literalObj);
//            }
            sketch.add(placeholderObj);
            literals.add(literalObj);
        }

        public Builder addPlaceholderSkipNull(@Nullable String placeholderObj, @Nullable Object literalObj) {
            if (placeholderObj == null) return this;
            if (literalObj == null) return this;
            return addPlaceholder(placeholderObj, literalObj);
        }

        public Builder addLiteral(@Nonnull String placeholderObj, @Nonnull Object literalObj) {
            Preconditions.checkNotNull(placeholderObj, "cant add null through this api");
            Preconditions.checkNotNull(literalObj, "cant add null through this api");
            addBoth(placeholderObj, literalObj);
            isLiteral.set(sketch.size() - 1);
            return this;
        }

        public Builder addLiteralSkipNull(@Nullable String placeholderObj, @Nullable Object literalObj) {
            if (literalObj == null) return this;
            if (placeholderObj == null) return this;
            return addLiteral(placeholderObj, literalObj);
        }

        public boolean isEmpty() {
            return sketch.isEmpty();
        }

        public FieldSketch build() {
            sketch.trim();
            literals.trim();
            return new FieldSketch(sketch, literals, isLiteral);
        }
    }

    private static final Joiner underJoiner = Joiner.on('_').useForNull("<null>");

    private final ObjectArrayList<String> sketch;
    private final ObjectArrayList<Object> literalFields;
    private final BitSet isLiteral;

    private FieldSketch(ObjectArrayList<String> sketch, ObjectArrayList<Object> literalFields, BitSet isLiteral) {
        this.sketch = sketch;
        this.literalFields = literalFields;
        this.isLiteral = isLiteral;
    }

    public boolean isLiteralValue(int componentIndex) {
        return isLiteral.get(componentIndex);
    }

    public String getComponentAsString(int componentIndex) {
        if (isLiteralValue(componentIndex)) {
            return literalFields.get(componentIndex).toString();
        }
        return sketch.get(componentIndex);
    }

    public String getSketchField(int fieldIndex) {
        return sketch.get(fieldIndex);
    }

    public Object getLiteralField(int fieldIndex) {
        return literalFields.get(fieldIndex);
    }

    public <T> T getComponentAs(int componentIndex, Class<T> valueType) {
        if (isLiteralValue(componentIndex)) {
            return Converters.convert(literalFields.get(componentIndex), valueType);
        }
        return Converters.convert(sketch.get(componentIndex), valueType);
    }

    public int size() {
        return sketch.size();
    }

    public boolean isEmpty() {
        return sketch.isEmpty();
    }

    public static boolean sketchEquivalent(FieldSketch a, FieldSketch b) {
        if (!a.sketch.equals(b.sketch)) {
            return false;
        }
        if (!a.isLiteral.equals(b.isLiteral)) {
            return false;
        }
        if (!a.isLiteral.isEmpty()) {
            // we need to iterate through the actual literal values to see if we're equiv
            for (int i = a.isLiteral.nextSetBit(0); i >= 0; i = a.isLiteral.nextSetBit(i+1)) {
                if (!Objects.equals(a.literalFields.get(i), b.literalFields.get(i))) {
                    return false;
                }
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
        }
        return true;
    }

    public static int sketchHashCode(FieldSketch a) {
        int total = 17;
        total = total * 37 + a.sketch.hashCode();
        total = total * 37 + a.isLiteral.hashCode();

        if (!a.isLiteral.isEmpty()) {
            // we need to iterate through the actual literal values to see if we're equiv
            for (int i = a.isLiteral.nextSetBit(0); i >= 0; i = a.isLiteral.nextSetBit(i+1)) {
                Object litObj = a.literalFields.get(i);
                if (litObj != null) {
                    total = total * 37 + litObj.hashCode();
                }
                if (i == Integer.MAX_VALUE) {
                    break; // or (i+1) would overflow
                }
            }
        }
        return total;
    }

    @Override
    public String toString() {
        if (sketch.isEmpty()) {
            return "<empty>";
        }
        return toSketchOnlyString();
    }

    public String toSketchOnlyString() {
        return underJoiner.join(sketch);
    }

    public String toSketchLiteralString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sketch.size(); i++) {
            if (i > 0) {
                sb.append("_");
            }
            sb.append(getComponentAsString(i));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldSketch that = (FieldSketch) o;

        return sketchEquivalent(this, that);
    }

    @Override
    public int hashCode() {
        return sketchHashCode(this);
    }
}
