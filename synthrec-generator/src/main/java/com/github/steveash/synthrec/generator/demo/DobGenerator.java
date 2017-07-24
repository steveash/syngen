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

package com.github.steveash.synthrec.generator.demo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.gen.TooManyRejectsSamplingException;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.base.Preconditions;

/**
 * Generates dates of birth given the age (using the basis date); note that here we're modeling the
 * distribution as P(year) * P(date of year) and then rejecting if the year + day of year is invalid
 * (which is just 2/29 on non leap years)
 * @author Steve Ash
 */
@LazyComponent
public class DobGenerator {

    private static final int MAX_TRIES = 10_000;
    @Resource  private int basisYear;

    private SamplingTable<String> daysOfYear;

    @PostConstruct
    protected void setup() {

        CsvTable table = CsvTable.loadSource(ReadWrite.findResource("dob/bday-days.csv"))
                .hasHeaders()
                .trimResults()
                .withSeparator('|')
                .build();
        MutableMultinomial<String> doys = new MutableMultinomial<>(-1);
        for (Row row : table) {
            String[] monthDay = row.getString("dayOfYear").split("/");
            Preconditions.checkState(monthDay.length == 2, "invalid entry ", row);
            int month = Integer.parseInt(monthDay[0]);
            int day = Integer.parseInt(monthDay[1]);
            doys.add(String.format("%02d-%02d", month, day), row.getInt("count"));
        }
        daysOfYear = SamplingTable.createFromMultinomial(doys);
    }

    public LocalDate generate(RandomGenerator rand, int age) {
        for (int i = 0; i < MAX_TRIES; i++) {

            String monthDay = daysOfYear.sampleWeighted(rand);
            int year = basisYear - age;
            try {
                return LocalDate.parse(year + "-" + monthDay);
            } catch (DateTimeParseException e) {
                // must be a leap year, try another sample
            }
        }
        throw new TooManyRejectsSamplingException();
    }
}
