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

import static com.github.steveash.synthrec.generator.feature.DobParsedFeature.DOB_PARSED_FEATURE;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;

/**
 * Feature of the age of the patient in # of years
 * @author Steve Ash
 */
@LazyComponent
public class AgeYearsFeature extends SingleFeatureComputer {
    private static final Logger log = LoggerFactory.getLogger(AgeYearsFeature.class);

    public static final FeatureKey<Integer> AGE_YEARS_FEATURE = new FeatureKey<>(Constants.AGE_YEARS, Integer.class);

    private final LocalDate basisDate;

    @Autowired
    public AgeYearsFeature(LocalDate basisDate) {
        super(AGE_YEARS_FEATURE, DOB_PARSED_FEATURE);
        this.basisDate = basisDate;
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        LocalDate parsed = record.getFeature(DOB_PARSED_FEATURE, null);
        if (parsed != null) {
            int years = (int) parsed.until(basisDate, ChronoUnit.YEARS);
            years = Math.max(0, years);
            if (years > 0) {
                sink.setFeature(AGE_YEARS_FEATURE, years);
            }
        }
    }
}
