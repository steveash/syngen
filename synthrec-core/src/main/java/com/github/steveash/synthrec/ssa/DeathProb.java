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

package com.github.steveash.synthrec.ssa;

import java.io.File;
import java.util.List;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.DataFiles;
import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * @author Steve Ash
 */
public class DeathProb {

    public static DeathProb makeDefault() {
        Int2ObjectMap<Int2DoubleArrayMap> male = readFile(DataFiles.load("dob/DeathProbsE_M_Alt2_TR2014.clob"));
        Int2ObjectMap<Int2DoubleArrayMap> female = readFile(DataFiles.load("dob/DeathProbsE_F_Alt2_TR2014.clob"));
        return new DeathProb(male, female);
    }

    private static Int2ObjectMap<Int2DoubleArrayMap> readFile(File csvFile) {
        CsvTable table = CsvTable.loadFile(csvFile)
                .skipFirst(1)
                .hasHeaders()
                .build();
        Int2ObjectMap<Int2DoubleArrayMap> basisToProb = new Int2ObjectOpenHashMap<>(120);
        List<String> headers = table.getHeaders();
        for (Row row : table) {
            Int2DoubleArrayMap ageDeathProb = new Int2DoubleArrayMap(headers.size());
            for (int i = 1; i < headers.size(); i++) {
                String ageString = headers.get(i);
                int age = Integer.parseInt(ageString);
                ageDeathProb.put(age, row.getDouble(ageString));
            }
            basisToProb.put(row.getInt(headers.get(0)), ageDeathProb);
        }
        return basisToProb;
    }

    private final Int2ObjectMap<Int2DoubleArrayMap> basisToAgeDeathMale;
    private final Int2ObjectMap<Int2DoubleArrayMap> basisToAgeDeathFemale;
    private final int minYear;
    private final int maxYear;
    private final int maxAge;

    private DeathProb(Int2ObjectMap<Int2DoubleArrayMap> basisToAgeDeathMale, 
            Int2ObjectMap<Int2DoubleArrayMap> basisToAgeDeathFemale) {
        this.basisToAgeDeathMale = basisToAgeDeathMale;
        this.basisToAgeDeathFemale = basisToAgeDeathFemale;
        int min = -1;
        int max = -1;
        int maxAge = 0;
        IntIterator iter = basisToAgeDeathMale.keySet().iterator();
        while (iter.hasNext()) {
            int next = iter.nextInt();
            if (min == -1) {
                min = max = next;
                continue;
            }

            min = Math.min(min, next);
            max = Math.max(max, next);
        }
        Int2DoubleArrayMap maxBasis = basisToAgeDeathMale.get(max);
        IntIterator iter1 = maxBasis.keySet().iterator();
        while (iter1.hasNext()) {
            int anAge = iter1.nextInt();
            maxAge = Math.max(maxAge, anAge);
        }
        this.minYear = min;
        this.maxYear = max;
        this.maxAge = maxAge;
    }

    public double deathProbBeforeAge(int basisYear, int age, boolean isMale) {
        double sum = 0;
        for (int i = 0; i < age; i++) {
            sum += deathProbAtAge(basisYear, age, isMale);
        }
        return sum;
    }
    
    public double deathProbAtAge(int basisYear, int age, boolean isMale) {
        if (age > maxAge) {
            return 1.0;
        }
        if (basisYear < minYear) {
            basisYear = minYear;
        } else if (basisYear > maxYear) {
            basisYear = maxYear;
        }
        Int2DoubleArrayMap deathProb;
        if (isMale) {
            deathProb = basisToAgeDeathMale.get(basisYear);
        } else {
            deathProb = basisToAgeDeathFemale.get(basisYear);
        }
        Preconditions.checkNotNull(deathProb, "cant find for basis", basisYear);
        return deathProb.get(age);
    }
}
