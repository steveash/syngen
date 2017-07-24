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

import static com.github.steveash.synthrec.generator.feature.AddressParserFeature.ADDRESS_PARSED_FEATURE;

import javax.annotation.Resource;

import com.github.steveash.synthrec.address.Address;
import com.github.steveash.synthrec.address.AddressSegment;
import com.github.steveash.synthrec.address.AddressTag;
import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.generator.reducer.SimpleTokenReducer;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.domain.FieldSketch.Builder;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.base.CharMatcher;

/**
 * Takes a parsed address and emits a feature for the sketch of the parsed address
 * @author Steve Ash
 */
@LazyComponent
public class AddressStructureFeature extends SingleFeatureComputer {

    public static final FeatureKey<FieldSketch> ADDRESS_STREET_STRUCT_FEATURE = new FeatureKey<>(Constants.ADDRESS_STREET_STRUCT, FieldSketch.class);

    @Resource private SimpleTokenReducer addressTokenReducer;

    public AddressStructureFeature() {
        super(ADDRESS_STREET_STRUCT_FEATURE, ADDRESS_PARSED_FEATURE);
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        Address address = record.getFeature(ADDRESS_PARSED_FEATURE, null);
        if (address == null) {
            return;
        }
        Builder builder = FieldSketch.builder();
        for (int i = 0; i < address.size(); i++) {
            AddressSegment seg = address.get(i);
            if (skip(seg.semanticTag)) {
                continue;
            }
            // we bin the word to address simple non-informational cases like 9B or all numbers
            String word = addressTokenReducer.reduceIfNecessary(seg.word);
            if (includeLiteral(seg.semanticTag)) {
                if (isGoodLiteral(seg.word)) {
                    builder.addLiteral(seg.semanticTag.toString(), word);
                } else {
                    builder.addPlaceholder(seg.semanticTag.toString(), word);
                }
            } else {
                builder.addPlaceholder(seg.semanticTag.toString(), word);
            }
        }
        sink.setFeature(ADDRESS_STREET_STRUCT_FEATURE, builder.build());
    }

    private boolean isGoodLiteral(String word) {
        return word.length() <= 4 && !CharMatcher.digit().matchesAnyOf(word);
    }

    private boolean skip(AddressTag semanticTag) {
        switch (semanticTag) {
            case City:
            case State:
            case Country:
            case Zip4:
            case Zip5:
            case Zip9:
                // would eventually like to be able to support recipient and C/O but for now skip
            case Recipient:
            case CoObject:
            case CoTag:
                return true;
            default:
                return false;
        }
    }

    private boolean includeLiteral(AddressTag semanticTag) {
        switch (semanticTag) {
            case AptTag:
            case HighwayTag:
            case CoTag:
            case RrBoxTag:
                return true;
            default:
                return false;
        }
    }
}
