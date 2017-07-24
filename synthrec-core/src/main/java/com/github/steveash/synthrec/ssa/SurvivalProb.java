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

import java.util.List;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.DataFiles;
import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;

/**
 * @author Steve Ash
 */
public class SurvivalProb {

    private final Int2DoubleArrayMap srv;
    private final int maxAge;

    public static SurvivalProb makeWithBasis(int basisYear) {
        CsvTable table = CsvTable.loadFile(DataFiles.load("dob/ssa-survival-fn.clob"))
                .hasHeaders()
                .build();
        List<String> headers = table.getHeaders();
        int cola = -1, colb = -1;
        double faca = 1.0, facb = 0.0;
        // find the columns surrounding the basis year
        for (int i = 1; i < headers.size() - 1; i++) {
            int yeara = Integer.parseInt(headers.get(i));
            int yearb = Integer.parseInt(headers.get(i + 1));
            Preconditions.checkState(yeara < yearb);
            if (basisYear >= yeara && basisYear < yearb) {
                // found the right ones; figure the interpolation weights
                double perc = ((double)(basisYear - yeara)) / ((double)(yearb - yeara));
                cola = i;
                colb = i +1;
                faca = 1.0 - perc;
                facb = perc;
                break;
            }
        }
        if (cola < 0) {
            // bigger or smaller
            int min = Integer.parseInt(headers.get(0));
            int max = Integer.parseInt(headers.get(headers.size() - 1));
            if (basisYear < min) {
                cola = colb = 0;
            } else if (basisYear >= max) {
                cola = colb = headers.size() - 1;
            }
        }
        Int2DoubleArrayMap srv = new Int2DoubleArrayMap(120);
        int maxAge = 0;
        for (Row row : table) {
            int age = row.getInt(0);
            double surva = row.getDouble(cola) / 100.0;
            double survb = row.getDouble(colb) / 100.0;
            Preconditions.checkState(surva >= 0 && surva <= 1.0);
            Preconditions.checkState(survb >= 0 && survb <= 1.0);
            double avg = (surva * faca) + (survb * facb);
            srv.put(age, avg);
            maxAge = Math.max(maxAge, age);
        }
        return new SurvivalProb(srv, maxAge);
    }

    private SurvivalProb(Int2DoubleArrayMap srv, int maxAge) {
        this.srv = srv;
        this.maxAge = maxAge;
    }

    public double probOfSurvivalToAge(int age) {
        if (age > maxAge) {
            return 0;
        }
        return srv.get(age);
    }
}
