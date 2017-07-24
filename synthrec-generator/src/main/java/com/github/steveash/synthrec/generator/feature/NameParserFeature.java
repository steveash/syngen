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

import static com.github.steveash.synthrec.Constants.FAMILY_NAME;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME;
import static com.github.steveash.synthrec.Constants.MIDDLE_NAME;
import static com.github.steveash.synthrec.Constants.SUFFIX_NAME;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.InputField;
import com.github.steveash.synthrec.name.NameEntryField;
import com.github.steveash.synthrec.name.NameTagger;
import com.github.steveash.synthrec.name.PersonalName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Feature that emits a parsed name from the name segments which it recreates by concatenating
 * @author Steve Ash
 */
@LazyComponent
public class NameParserFeature extends SingleFeatureComputer {

    public static final FeatureKey<PersonalName> NAME_PARSED_FEATURE = new FeatureKey<>(Constants.NAME_PARSED, PersonalName.class);

    private final NameTagger nameTagger;

    @Autowired
    public NameParserFeature(NameTagger nameTagger) {
        super(NAME_PARSED_FEATURE);
        this.nameTagger = nameTagger;
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        List<InputField> inputs = reconstructFullName(record);
        if (inputs.isEmpty()) {
            return;
        }
        PersonalName parse = nameTagger.parse(inputs);
        if (parse.getTokens().isEmpty()) {
            // couldnt parse so assume garbage and dont bother registering a feature value
            return;
        }
        sink.setFeature(NAME_PARSED_FEATURE, parse);
    }

    private List<InputField> reconstructFullName(ReadableRecord record) {
        ArrayList<InputField> result = Lists.newArrayList();
        ImmutableList<String> segs = Constants.NAME_SEGMENTS_SUFFIX;
        for (int i = 0; i < segs.size(); i++) {
            String segment = segs.get(i);
            String value = record.getField(segment, "");
            if (isNotBlank(value)) {
                result.add(new InputField(value, segTofield(segment)));
            }
        }
        return result;
    }

    private NameEntryField segTofield(String segment) {
        switch (segment) {
            case GIVEN_NAME:
                return NameEntryField.FirstName;
            case MIDDLE_NAME:
                return NameEntryField.MiddleName;
            case FAMILY_NAME:
                return NameEntryField.LastName;
            case SUFFIX_NAME:
                return NameEntryField.Suffix;
        }
        throw new IllegalStateException("dont know " + segment);
    }
}
