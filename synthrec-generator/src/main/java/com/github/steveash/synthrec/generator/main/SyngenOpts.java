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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * Command line options for running syngen
 * @author Steve Ash
 */
public class SyngenOpts {

    @Option(name = "--mapSample")
    public boolean runMappingTest = false;

    @Option(name = "--qualityStats", usage = "Generates the quality stats output for the activated profile")
    public boolean runDqStats = false;

    @Option(name ="--profile", usage = "Runs the count profiling")
    public boolean runProfile = false;

    @Option(name = "--profileReport", usage ="The file name of the output profiling report to create")
    public String profileReport;

    @Option(name ="--deident", usage = "Runs the deidentification process on the profile outputs")
    public boolean runDeidentify = false;

    @Option(name = "--deidentReport", usage = "The base file name of the output deident reports")
    public String deidentReport;

    @Option(name ="--genRecords", usage = "Runs gold record generation from profile/deident output")
    public boolean runGenRecords = false;

    @Option(name ="--goldRecords", usage = "The location to write generated gold records or to read in pair generation")
    public String goldRecordsFile;

    @Option(name ="--countDag", usage="The path to the count dag serialized file that contains the " +
            "result of profiling or deident OR the input to genRecords")
    public String countDagFile;

    @Argument
    private List<String> arguments = new ArrayList<>();

    @Option(name="--help", aliases = {"/?"}, usage = "Display the usage options", help = true)
    public boolean help;

    // apply defaults etc
    public void afterPropertiesSet() {
//        if (runProfile && isBlank(profileReport)) {
//            profileReport = "profiling-results.txt";
//        }
//        if (runDeidentify && isBlank(deidentReport)) {
//            deidentReport = "deident-results";
//        }
        if (isBlank(goldRecordsFile)) {
            goldRecordsFile = "goldrecords.csv";
        }
        if (isBlank(countDagFile)) {
            countDagFile = "profile-dag.dat";
        }
        if (isBlank(deidentReport)) {
            deidentReport = "countdag-report.txt";  //
        }
    }
}
