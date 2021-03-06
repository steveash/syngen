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

package com.github.steveash.synthrec.generator.gen;

import java.util.Set;

import javax.annotation.Resource;

import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.gen.ConditionalUnaryNode;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.BackoffSmoother;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
@Deprecated // not sure what this was originally for, i can imagine i might need it later
@LazyComponent
public class ConditionalUnaryGenNodeProvider implements GenNodeProvider {

    @Resource private GenRecordsConfig genRecordsConfig;

    @Override
    public GenNode makeFor(String name, CountDag countDag) {
        FactorGroup group = countDag.getFactorGroup(name);

        BackoffSmoother<?> smoother = BackoffSmoother.startingWith(
                genRecordsConfig.getDefaultPriorAlpha(),
                genRecordsConfig.getDefaultPriorMinVirtual(),
                group.getFactorParentsNameAsSet()
        ).withBaseName(name)
                .build();

        Preconditions.checkState(group.getFactorNonParentsName().size() == 1, "must be unary");
        Preconditions.checkState(group.getFactorParentsName().size() >= 1, "must be conditional");
        return new ConditionalUnaryNode(group.getFactorNonParentsName().get(0),
                group.getFactorParentsNameAsSet(),
                smoother.smoothSampler(group.makeConditionalCopy())
        );
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of();
    }
}
