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

package com.github.steveash.synthrec.generator.feature;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.PrototypeComponent;
import com.github.steveash.synthrec.string.PatternReducer;

/**
 * Reduces a string fields into a "pattern" representation which reduces all letters and case to A all digits to 9
 * except 0, collapses and trims whitespace and cleans up surrounding whitespace around certain punctuation combinations
 *
 * @author Steve Ash
 */
@PrototypeComponent
public class PatternFeature extends SingleFeatureComputer {

    public static FeatureKey<String> keyFor(String featureKey) {
        return new FeatureKey<>(featureKey, String.class);
    }

    public PatternFeature(String fieldName, String featureKey) {
        super(keyFor(featureKey));
        this.fieldName = fieldName;
        this.featureKey = keyFor(featureKey);
    }

    private final FeatureKey<String> featureKey;
    private final String fieldName;

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        String field = record.getField(fieldName, null);
        if (isBlank(field)) {
            return;
        }
        String pattern = PatternReducer.replace(field);
        if (isNotBlank(pattern)) {
            sink.setFeature(featureKey, pattern);
        }
    }
}
