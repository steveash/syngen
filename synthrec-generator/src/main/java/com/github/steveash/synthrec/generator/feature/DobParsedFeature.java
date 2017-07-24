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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.DateTimeException;
import java.time.LocalDate;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;

/**
 * Feature of the parsed date as a LocalDate
 * @author Steve Ash
 */
@LazyComponent
public class DobParsedFeature extends SingleFeatureComputer {

    public static final FeatureKey<LocalDate> DOB_PARSED_FEATURE = new FeatureKey<>(Constants.DOB_PARSED,
            LocalDate.class
    );

    public DobParsedFeature() {
        super(DOB_PARSED_FEATURE);
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        // the normalized form is "yyyy-mm-dd"
        String dob = record.getNormal(Constants.DOB, null);
        if (isNotBlank(dob)) {
            LocalDate parsed;
            try {
                parsed = LocalDate.parse(dob);
            } catch (DateTimeException e) {
                // can't parse the dob so just don't emit
                return;
            }
            sink.setFeature(DOB_PARSED_FEATURE, parsed);
        }
    }
}
