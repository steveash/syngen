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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.right;

import java.io.File;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.stat.ConcurrentCounter;

/**
 * @author Steve Ash
 */
public class DobFieldProfiler implements FieldProfiler {

    private final ConcurrentCounter<String> years = new ConcurrentCounter<>();
    private final ConcurrentCounter<String> monthDay = new ConcurrentCounter<>();

    @Override
    public void onValue(ReadableRecord record) {
        String dobString = record.getNormal(Constants.DOB, null);
        // yyyy-mm-dd is the normalized version
        if (isBlank(dobString) || dobString.length() != 10) {
            return;
        }
        years.increment(left(dobString, 4));
        monthDay.increment(right(dobString, 5));
    }

    @Override
    public void finish(Context context) {
        ReadWrite.writeCountTable(years.drainTo(),
                new File(context.getOutputDir(), context.getMoniker() + ".dobyears.psv"),
                "|"
        );
        ReadWrite.writeCountTable(monthDay.drainTo(),
                new File(context.getOutputDir(), context.getMoniker() + ".dobmonthday.psv"),
                "|"
        );
    }
}
