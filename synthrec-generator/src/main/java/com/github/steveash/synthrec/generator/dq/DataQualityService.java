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

package com.github.steveash.synthrec.generator.dq;

import static com.github.steveash.synthrec.generator.feature.AddressStructureFeature.ADDRESS_STREET_STRUCT_FEATURE;
import static com.github.steveash.synthrec.generator.feature.NameStructureFeature.FAMILY_NAME_STRUCT_FEATURE;
import static com.github.steveash.synthrec.generator.feature.NameStructureFeature.GIVEN_NAME_STRUCT_FEATURE;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.FeatureComputer;
import com.github.steveash.synthrec.domain.NullFeatureComputer;
import com.github.steveash.synthrec.generator.DqConfig;
import com.github.steveash.synthrec.generator.dq.FieldProfiler.Context;
import com.github.steveash.synthrec.generator.enrich.FeatureService;
import com.github.steveash.synthrec.generator.feature.AddressStructureFeature;
import com.github.steveash.synthrec.generator.feature.NameStructureFeature;
import com.github.steveash.synthrec.generator.load.InputFile;
import com.github.steveash.synthrec.generator.load.InputPipeline;
import com.github.steveash.synthrec.generator.spring.PrototypeComponent;
import com.github.steveash.synthrec.util.StreamCounter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
@PrototypeComponent
public class DataQualityService {
    private static final Logger log = LoggerFactory.getLogger(DataQualityService.class);

    @Resource private InputPipeline inputPipeline;
    @Resource private DqConfig dqConfig;
    @Resource private MetricRegistry metricRegistry;
    @Resource private FeatureService featureService;
    @Value("${synthrec.input.moniker}") private String moniker;
    @Value("${synthrec.output-folder}") private String outputLocation;

    private Meter profileRecord;
    private Collection<FieldProfiler> profilers = Lists.newArrayList();
    private FeatureComputer computer = NullFeatureComputer.INSTANCE;

    @PostConstruct
    protected void setup() {
        profileRecord = metricRegistry.meter("profileRecord");
    }

    public void execute(InputFile records) {
        makeValueProfilers();
        Stopwatch watch = Stopwatch.createStarted();
        log.info("Starting " + moniker + " to profile " + dqConfig.getCountFields()
                .size() + " fields for data quality...");
        ImmutableList<? extends FieldProfiler> profilers = ImmutableList.copyOf(this.profilers);

        long count = inputPipeline.normalizedFrom(records).parallel()
                .map(rec -> {computer.emitFeatures(rec, rec); return rec;})
                .map(rec -> {
                    for (FieldProfiler profiler : profilers) {
                        profiler.onValue(rec);
                    }
                    profileRecord.mark();
                    return rec;
                })
                .map(new StreamCounter<>("Data quality profiling"))
                .count();

        log.info("completed profiling, now outputting records...");
        outputResults();

        watch.stop();
        log.info("Completed all data profiling of " + count + " records in " + watch.toString());
    }

    private void outputResults() {
        new File(outputLocation).mkdirs();
        Context context = new Context(new Date(), this.moniker, outputLocation);
        for (FieldProfiler profiler : profilers) {
            profiler.finish(context);
        }
    }

    private void makeValueProfilers() {
        profilers.clear();
        for (String field : dqConfig.getCountFields()) {
            profilers.add(new ValueProfiler(true, field));
        }
        // we have some special fields for name and address
        profilers.add(new BinnedWordProfiler(true, "name-bin-tokens", Constants.ALL_NAME_SEGMENTS));
        profilers.add(new ManyProfiler(true, "full-names", Constants.ALL_NAME_SEGMENTS));
        profilers.add(new BinnedWordProfiler(true, "address-bin-tokens", ImmutableList.of(Constants.ADDRESS)));
        profilers.add(new ManyProfiler(true, "full-addresses", ImmutableList.of(Constants.ADDRESS)));
        profilers.add(new DobFieldProfiler());
        profilers.add(new PresentFieldProfiler(ImmutableList.of(
                Constants.GIVEN_NAME, Constants.MIDDLE_NAME, Constants.FAMILY_NAME, Constants.SUFFIX_NAME,
                Constants.ADDRESS_STREET, Constants.ADDRESS_CITY, Constants.ADDRESS_STATE, Constants.ADDRESS_ZIP,
                Constants.SEX, Constants.PHONE, Constants.DOB, Constants.SSN
        )));
        profilers.add(new TokenProfiler("given-name-tokens", ImmutableList.of(Constants.GIVEN_NAME, Constants.MIDDLE_NAME)));
        profilers.add(new TokenProfiler("family-name-tokens", ImmutableList.of(Constants.FAMILY_NAME)));
        profilers.add(new TokenProfiler("address-street-tokens", ImmutableList.of(Constants.ADDRESS_STREET)));

        computer = featureService.computersForAsComposite(ImmutableSet.of(
                GIVEN_NAME_STRUCT_FEATURE,
                FAMILY_NAME_STRUCT_FEATURE,
                ADDRESS_STREET_STRUCT_FEATURE
        ));

        profilers.add(new FieldSketchProfiler(true, "name-struct", ImmutableList.of(
                GIVEN_NAME_STRUCT_FEATURE, FAMILY_NAME_STRUCT_FEATURE)));
        profilers.add(new FieldSketchProfiler(true, "address-struct", ImmutableList.of(ADDRESS_STREET_STRUCT_FEATURE)));
    }
}
