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

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.gen.ConditionalMultipleNode;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.BackoffSmoother;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author Steve Ash
 */
@LazyComponent
public class ConditionalMultipleGenNodeProvider implements GenNodeProvider {

    @Resource private GenRecordsConfig genRecordsConfig;

    // TODO theres really no reason that we need to encode the structure here; the countdag already knows
        // whats independent and conditional and joint; just use that to delegate to the proper
        // general gen node (for things that aren't already overriden)

    // if you need to translate a name passed into the input to the actual factor
    // group that you want to use to emit
    private static final ImmutableMap<String,String> NAME_INPUT_TO_GROUP = ImmutableMap.of(
        Constants.GIVEN_NAME_CULTURE, Constants.GIVEN_FAMILY_JOINT,
        Constants.FAMILY_NAME_CULTURE, Constants.GIVEN_FAMILY_JOINT
    );

    @Override
    public GenNode makeFor(String name, CountDag countDag
    ) {
        name = NAME_INPUT_TO_GROUP.getOrDefault(name, name);

        FactorGroup group = countDag.getFactorGroup(name);
        BackoffSmoother<?> smoother = BackoffSmoother.startingWith(
                genRecordsConfig.getDefaultPriorAlpha(),
                genRecordsConfig.getDefaultPriorMinVirtual(),
                group.getFactorParentsNameAsSet()
        ).withBaseName(name)
                .build();
        Preconditions.checkState(group.getFactorNonParentsName().size() == 2, "must be multiple");
        Preconditions.checkState(group.getFactorParentsName().size() >= 1, "must be conditional");

        return new ConditionalMultipleNode(group.getFactorNonParentsName(),
                group.getFactorParentsNameAsSet(),
                smoother.smoothSampler(group.makeConditionalCopy())
        );
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of(
                Constants.GIVEN_FAMILY_JOINT,
                Constants.GIVEN_NAME_CULTURE,
                Constants.FAMILY_NAME_CULTURE
        );
    }
}
