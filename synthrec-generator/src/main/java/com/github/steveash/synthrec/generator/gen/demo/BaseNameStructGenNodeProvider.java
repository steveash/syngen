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
import com.github.steveash.synthrec.gen.ConditionalUnaryNode;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.stat.BackoffSmoother;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Does the work of creating the struct gen node (and smoothing to age, etc.)
 * @author Steve Ash
 */
public abstract class BaseNameStructGenNodeProvider implements GenNodeProvider {

    private final String structOutputKey;
    private final String cultureInputKey;

    @Resource private GenRecordsConfig genRecordsConfig;

    protected BaseNameStructGenNodeProvider(String structOutputKey, String cultureInputKey) {
        this.structOutputKey = structOutputKey;
        this.cultureInputKey = cultureInputKey;
    }

    @Override
    public GenNode makeFor(String name, CountDag countDag) {
        FactorGroup group = countDag.getFactorGroup(name);
        Preconditions.checkState(group.getFactorNonParentsName().size() == 1, "must be unary");
        ImmutableSet<String> parents = ImmutableSet.of(cultureInputKey, Constants.SEX);
        Preconditions.checkState(group.getFactorParentsNameAsSet().equals(parents),
                "expecting a different set of parents"
        );

        // smooth culture + sex, backoff to sex, finally backoff to unconditional
        BackoffSmoother<?> smoother = BackoffSmoother.startingWith(
                genRecordsConfig.getDefaultPriorAlpha(),
                genRecordsConfig.getDefaultPriorMinVirtual(),
                parents
        )
                .nextOn(Constants.SEX)
                .withBaseName(name)
                .build();

        return new ConditionalUnaryNode(group.getFactorNonParentsName().get(0),
                group.getFactorParentsNameAsSet(),
                smoother.smoothSampler(group.makeConditionalCopy())
        );
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of(structOutputKey);
    }
}
