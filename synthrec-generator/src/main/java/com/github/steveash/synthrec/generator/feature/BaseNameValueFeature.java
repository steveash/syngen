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

import static com.github.steveash.synthrec.generator.feature.NameParserFeature.NAME_PARSED_FEATURE;

import java.util.function.Predicate;

import com.github.steveash.synthrec.domain.MultivalueString;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.name.NameToken;
import com.github.steveash.synthrec.name.PersonalName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * A feature to take the parsed name and emit tokens as a Multivalue
 * @author Steve Ash
 */
public class BaseNameValueFeature extends SingleFeatureComputer {

    private final Predicate<NamePart> accepted;
    private final FeatureKey<MultivalueString> outputKey;

    public BaseNameValueFeature(Predicate<NamePart> accepted, FeatureKey<MultivalueString> singleOutputKey) {
        super(singleOutputKey, NameParserFeature.NAME_PARSED_FEATURE);
        this.accepted = accepted;
        this.outputKey = singleOutputKey;
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        PersonalName parsed = record.getFeature(NAME_PARSED_FEATURE, null);
        if (parsed == null) {
            return;
        }
        Builder<String> tokens = ImmutableList.builder();
        for (int i = 0; i < parsed.size(); i++) {
            NameToken nameToken = parsed.get(i);
            if (nameToken.getPart() != null && accepted.test(nameToken.getPart())) {
                tokens.add(NameStructureFeature.targetToken(nameToken));
            }
        }
        ImmutableList<String> result = tokens.build();
        if (!result.isEmpty()) {
            sink.setFeature(outputKey, new MultivalueString(result));
        }
    }
}
