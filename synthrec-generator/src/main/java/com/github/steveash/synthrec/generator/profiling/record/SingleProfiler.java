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

package com.github.steveash.synthrec.generator.profiling.record;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.collect.LazyMap;
import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.generator.load.InputFile;
import com.github.steveash.synthrec.generator.spring.PrototypeComponent;

/**
 * @author Steve Ash
 */
@PrototypeComponent
public class SingleProfiler {
    private static final Logger log = LoggerFactory.getLogger(SingleProfiler.class);

    private final InputFile inputFile;

    private final Map<String,SummaryStatistics> fields = LazyMap.makeSupply(SummaryStatistics::new);

    public SingleProfiler(InputFile inputFile) {this.inputFile = inputFile;}

    public void profile() {
        for (Record record : inputFile) {
            for (Entry<String, String> entry : record.fields().entrySet()) {
                SummaryStatistics stats = fields.get(entry.getKey());
                stats.addValue(entry.getValue().length());
            }
        }
    }

    public void printResults() {
        for (Entry<String, SummaryStatistics> entry : fields.entrySet()) {
            log.info("Field: " + entry.getKey() + " " + entry.getValue().toString());
        }
    }
}
