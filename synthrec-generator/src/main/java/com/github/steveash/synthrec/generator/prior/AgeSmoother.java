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

package com.github.steveash.synthrec.generator.prior;

import javax.annotation.Resource;

import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.ssa.AgeDist;
import com.github.steveash.synthrec.stat.EmpPriorSmoother;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.SamplingTable;

/**
 * Smooths an age distribution by using the interpolated census age distribution
 * @author Steve Ash
 */
@LazyComponent
public class AgeSmoother {

    @Resource private GenRecordsConfig genRecordsConfig;
    @Resource private AgeDist ageDist;

    public SamplingTable<Integer> smoothEmpirical(Multinomial<Integer> empirical) {

        EmpPriorSmoother smoother = new EmpPriorSmoother(genRecordsConfig.getAgePriorAlpha(),
                genRecordsConfig.getAgePriorMinVirtual());
        MutableMultinomial<Integer> result = ageDist.getAgeNormalized().copy();
        smoother.smoothPriorCopy(empirical, result, "age");
        return SamplingTable.createFromMultinomial(result);
    }
}
