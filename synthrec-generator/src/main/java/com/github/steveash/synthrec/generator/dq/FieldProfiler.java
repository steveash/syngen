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

import java.util.Date;

import com.github.steveash.synthrec.domain.ReadableRecord;

/**
 * General profiler interface contract
 * @author Steve Ash
 */
public interface FieldProfiler {

    void onValue(ReadableRecord record);

    void finish(Context context);

    public static class Context {

        private final Date runTime;
        private final String moniker;
        private final String outputDir;

        public Context(Date runTime, String moniker, String outputDir) {
            this.runTime = runTime;
            this.moniker = moniker;
            this.outputDir = outputDir;
        }

        public Date getRunTime() {
            return runTime;
        }

        public String getMoniker() {
            return moniker;
        }

        public String getOutputDir() {
            return outputDir;
        }
    }
}
