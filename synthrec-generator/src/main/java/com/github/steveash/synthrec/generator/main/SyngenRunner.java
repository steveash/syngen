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

package com.github.steveash.synthrec.generator.main;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.deident.DeidentRecordService;
import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.generator.dq.DataQualityService;
import com.github.steveash.synthrec.generator.gen.GenRecordService;
import com.github.steveash.synthrec.generator.load.InputFile;
import com.github.steveash.synthrec.generator.load.InputFileFactory;
import com.github.steveash.synthrec.generator.load.InputPipeline;
import com.github.steveash.synthrec.generator.profiling.count.RecordProfilingService;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

/**
 * A runner for the counter which counts distributions in a source input file
 * @author Steve Ash
 */
@Profile(SyngenRunner.SYNCLI)
@LazyComponent
public class SyngenRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SyngenRunner.class);
    public static final String SYNCLI = "cli";

    @Lazy @Resource private InputFileFactory inputFileFactory;
    @Lazy @Resource private RecordProfilingService recordProfilingService;
    @Lazy @Resource private InputPipeline inputPipeline;
    @Lazy @Resource private DeidentRecordService deidentRecordService;
    @Lazy @Resource private GenRecordService genRecordService;
    @Lazy @Resource private DataQualityService dataQualityService;

    @Value("${synthrec.output-folder}") private String outputLocation;
    @Value("${synthrec.input.moniker}") private String moniker;

    private SyngenOpts opts;
    private CountDag countDag;

    @Override
    public void run(String... args) throws Exception {
        opts = new SyngenOpts();
        CmdLineParser parser = new CmdLineParser(opts);
        try {
            parser.parseArgument(args);
            opts.afterPropertiesSet();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            return;
        }
        execute();
    }

    @Lookup
    protected DataQualityService newDataQualityService() {
        throw new IllegalStateException();
    }

    private void execute() throws IOException {

        if (opts.runMappingTest) {
            log.info("Writing out some records to test the mapping");
            writeSampleRecords();
        }

        if (opts.runDqStats) {
            log.info("Run data quality profiling analysis");
            InputFile records = inputFileFactory.makeDefault();
            newDataQualityService().execute(records);
        }

        if (opts.runProfile) {
            log.info("Profiling records ...");
            InputFile records = inputFileFactory.makeDefault();
            log.info("Reading from input file " + records.getConfig().getResource());

            countDag = recordProfilingService.execute(records);
            writeProfileReport(countDag);
            writeCountDag(countDag);
        }

        if (opts.runDeidentify) {
            log.info("Deidentifying records ...");
            loadCountDag();
            deidentRecordService.deident(countDag, Strings.emptyToNull(opts.deidentReport));
            writeCountDag(countDag);
        }

        if (opts.runGenRecords) {
            log.info("Generating gold records...");
            loadCountDag();
            log.info("Writing gold records to: " + opts.goldRecordsFile);
            CharSink charSink = Files.asCharSink(outputFile(opts.goldRecordsFile), Charsets.UTF_8);
            genRecordService.generate(countDag, charSink);
        }
        log.info("Syngen has completed all tasks");
    }

    private void writeSampleRecords() {
        InputFile records = inputFileFactory.makeDefault();
        File orig = new File(outputLocation, moniker + ".sample.origin.txt");
        File norm = new File(outputLocation, moniker + ".sample.normal.txt");
        Joiner joiner = Joiner.on('|');
        try (PrintWriter origPw = new PrintWriter(orig);
             PrintWriter normPw = new PrintWriter(norm)) {

            List<Record> recs = inputPipeline.normalizedFrom(records).limit(100).collect(Collectors.toList());
            List<String> headers = recs.stream().flatMap(r -> r.fields().keySet().stream()).distinct().sorted().collect(Collectors.toList());
            origPw.println(joiner.join(headers));
            normPw.println(joiner.join(headers));
            for (Record rec : recs) {
                origPw.println(joiner.join(headers.stream().map(r -> rec.getField(r, "")).collect(Collectors.toList())));
                normPw.println(joiner.join(headers.stream().map(r -> rec.getNormal(r, "")).collect(toList())));
            }
            log.info("Printed samples to " + orig);

        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void loadCountDag() {
        if (countDag == null) {
            log.info("Reading countdag from " + opts.countDagFile);
            countDag = ReadWrite.objectFromGzip(opts.countDagFile, CountDag.class);
        }
    }

    private void writeCountDag(CountDag countDag) {
        if (isNotBlank(opts.countDagFile)) {
            log.info("Writing CoutDag out to " + opts.countDagFile + " ...");
            ReadWrite.objectToGzip(countDag, new File(opts.countDagFile));
        }
    }

    private void writeProfileReport(CountDag countDag) throws FileNotFoundException {
        if (isNotBlank(opts.profileReport)) {
            try (PrintWriter pw = new PrintWriter(outputFile(opts.profileReport))) {
                countDag.printTo(pw);
            }
        }
    }

    private File outputFile(String filename) {
        File maybeAbsolute = new File(filename);
        if (maybeAbsolute.isAbsolute()) {
            return new File(filename);
        }
        new File(outputLocation).mkdirs();
        return new File(outputLocation, filename);
    }
}
