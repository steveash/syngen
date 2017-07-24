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

import static com.github.steveash.synthrec.Constants.FAMILY_NAME_STRUCT;
import static com.github.steveash.synthrec.generator.feature.NameParserFeature.NAME_PARSED_FEATURE;
import static com.github.steveash.synthrec.name.NamePart.AKA;
import static com.github.steveash.synthrec.name.NamePart.And;
import static com.github.steveash.synthrec.name.NamePart.Unknown;

import java.util.Set;

import javax.annotation.Resource;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.FeatureComputer;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.domain.FieldSketch.Builder;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.ChunkedName;
import com.github.steveash.synthrec.name.NameChunker;
import com.github.steveash.synthrec.name.NameEntryField;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.name.NameSegment;
import com.github.steveash.synthrec.name.NameToken;
import com.github.steveash.synthrec.name.PersonalName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Feature that emits a FieldSketch for the given name and family name chunks from the parsed name
 * @author Steve Ash
 */
@LazyComponent
public class NameStructureFeature implements FeatureComputer {

    public static final FeatureKey<FieldSketch> GIVEN_NAME_STRUCT_FEATURE = new FeatureKey<>(Constants.GIVEN_NAME_STRUCT, FieldSketch.class);
    public static final FeatureKey<FieldSketch> FAMILY_NAME_STRUCT_FEATURE = new FeatureKey<>(FAMILY_NAME_STRUCT, FieldSketch.class);

    private static final ImmutableSet<NamePart> literals = ImmutableSet.<NamePart>builder()
            .add(And).add(AKA).add(Unknown)
            .build();

    public static NamePart getNamePartFromLabel(String label) {
        int idx = label.indexOf('@');
        Preconditions.checkState(idx > 0, "invalid label", label);
        return NamePart.valueOf(label.substring(0, idx));
    }

    public static NameEntryField getEntryFieldFromLabel(String label) {
        int idx = label.indexOf('@');
        Preconditions.checkState(idx > 0, "invalid label", label);
        return NameEntryField.valueOf(label.substring(idx + 1));
    }

    public static String makeLabelFrom(NamePart part, NameEntryField entryField) {
        return part.toString() + "@" + entryField.toString();
    }

    @Resource private NameChunker nameChunker;

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        PersonalName parsed = record.getFeature(NAME_PARSED_FEATURE, null);
        if (parsed == null) {
            return;
        }
        // given name sketch
        Builder given = FieldSketch.builder();
        Builder family = FieldSketch.builder();
        ChunkedName chunked = nameChunker.chunk(parsed);
        for (int i = 0; i < parsed.size(); i++) {
            NameToken token = parsed.get(i);
            Builder addTo = given;
            NameSegment segment = chunked.segmentAt(i);
            if (segment == NameSegment.Family) {
                addTo = family;
            }
            NamePart tokenPart = token.getPart();
            String label = makeLabelFrom(tokenPart, token.getEntryField());
            String val = mask(tokenPart, targetToken(token));
            if (literals.contains(tokenPart) && isGoodLiteral(token.getNormalNoPunc())) {
                addTo.addLiteral(label, val);
            } else {
                addTo.addPlaceholder(label, val);
            }
        }
        if (!given.isEmpty()) {
            sink.setFeature(GIVEN_NAME_STRUCT_FEATURE, given.build());
        }
        if (!family.isEmpty()) {
            sink.setFeature(FAMILY_NAME_STRUCT_FEATURE, family.build());
        }
    }

    private String mask(NamePart tokenPart, String value) {
        // we're omitting this because we are emitting it instead as GIVEN NAMEISH (etc)
        if (NamePart.isGivenIdentifying(tokenPart) || NamePart.isSurnameIdentifying(tokenPart)) {
            return Constants.MISSING;
        }
        return value;
    }

    static String targetToken(NameToken token) {
        return token.getOriginal().toUpperCase().trim();
    }

    private boolean isGoodLiteral(String token) {
        return (token.length() <= 3);
    }

    @Override
    public Set<FeatureKey<?>> requires() {
        return ImmutableSet.of(NAME_PARSED_FEATURE);
    }

    @Override
    public Set<FeatureKey<?>> satisfies() {
        return ImmutableSet.of(GIVEN_NAME_STRUCT_FEATURE, FAMILY_NAME_STRUCT_FEATURE);
    }
}
