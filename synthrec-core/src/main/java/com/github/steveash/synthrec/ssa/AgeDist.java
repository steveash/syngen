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

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;

/**
 * This captures the census age/sex distribution
 * @author Steve Ash
 */

public class AgeDist {

    public static AgeDist create() {
        CsvTable table = CsvTable.loadSource(ReadWrite.findResource("dob/age-dist-2013.csv"))
                .hasHeaders()
                .build();
        MutableMultinomial<Integer> ageDist = new MutableMultinomial<>(-1);
        for (Row row : table) {
            int age = row.getInt(0);
            double population = row.getDouble(1);
            ageDist.add(age, population);
        }
        return new AgeDist(ageDist.normalize().toImmutable());
    }

    private final Multinomial<Integer> ageNormalized;

    private AgeDist(Multinomial<Integer> ageNormalized) {this.ageNormalized = ageNormalized;}

    public Multinomial<Integer> getAgeNormalized() {
        return ageNormalized;
    }
}
