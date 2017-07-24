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

package com.github.steveash.synthrec.generator.gen.demo;

import static com.github.steveash.synthrec.Constants.AGE_YEARS;

import java.util.Set;

import javax.annotation.Resource;

import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.gen.UnaryNode;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.prior.AgeSmoother;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.ImmutableSet;

/**
 * Provides gen node for the age years factor, provides some smoothing
 * @author Steve Ash
 */
@LazyComponent
public class AgeYearsGenNodeProvider implements GenNodeProvider {


    @Resource private AgeSmoother ageSmoother;

    @Override
    public GenNode makeFor(String name, CountDag countDag) {
        FactorGroup group = countDag.getFactorGroup(name);
        return new UnaryNode(group.getName(), ageSmoother.smoothEmpirical(group.makeUnconditionalUnaryCopy()));
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of(AGE_YEARS);
    }
}
