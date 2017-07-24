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

import java.util.Set;

import javax.annotation.Resource;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.stat.BackoffSampler;
import com.github.steveash.synthrec.gen.ConditionalUnaryNode;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.prior.ZipSmoother;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
@LazyComponent
public class CityBinGenNodeProvider implements GenNodeProvider {


    @Resource private ZipSmoother zipSmoother;

    @Override
    public GenNode makeFor(String name, CountDag countDag) {
        FactorGroup group = countDag.getFactorGroup(name);
        BackoffSampler sampling = zipSmoother.smoothCity(
                group.makeConditionalCopy(),
                group.getFactorParentsNameAsSet()
        );
        return new ConditionalUnaryNode(group.getName(),
                group.getFactorParentsNameAsSet(),
                sampling
        );
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of(Constants.ADDRESS_CITY_BIN);
    }
}
