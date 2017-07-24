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
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Set;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.FeatureComputer;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.PersonalName;
import com.google.common.collect.ImmutableSet;

/**
 * Emits the cutlure of origin which is estimated from the culture estimates of the name tokens (scaled by
 * the entropy of the predicted distributions to bias towards confidence)
 * @author Steve Ash
 */
@LazyComponent
public class NameCultureFeature implements FeatureComputer {

    public static final FeatureKey<String> CULTURE_FEATURE = new FeatureKey<>(Constants.ORIGIN_CULTURE, String.class);
    public static final FeatureKey<String> GIVEN_NAME_CULTURE_FEATURE = new FeatureKey<>(Constants.GIVEN_NAME_CULTURE, String.class);
    public static final FeatureKey<String> FAMILY_NAME_CULTURE_FEATURE = new FeatureKey<>(Constants.FAMILY_NAME_CULTURE, String.class);

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        PersonalName parsed = record.getFeature(NAME_PARSED_FEATURE, null);
        if (parsed == null) {
            return;
        }
        String culture = parsed.getCulture();
        if (isNotBlank(culture)) {
            sink.setFeature(CULTURE_FEATURE, culture);
        }
        String givenCulture = parsed.getGivenNameCulture();
        if (isNotBlank(givenCulture)) {
            sink.setFeature(GIVEN_NAME_CULTURE_FEATURE, givenCulture);
        }
        String familyCulture = parsed.getFamilyNameCulture();
        if (isNotBlank(familyCulture)) {
            sink.setFeature(FAMILY_NAME_CULTURE_FEATURE, familyCulture);
        }
    }

    @Override
    public Set<FeatureKey<?>> requires() {
        return ImmutableSet.of(NAME_PARSED_FEATURE);
    }

    @Override
    public Set<FeatureKey<?>> satisfies() {
        return ImmutableSet.of(CULTURE_FEATURE, GIVEN_NAME_CULTURE_FEATURE, FAMILY_NAME_CULTURE_FEATURE);
    }
}
