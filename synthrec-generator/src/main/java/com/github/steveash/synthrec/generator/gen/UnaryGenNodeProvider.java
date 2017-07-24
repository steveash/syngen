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

import static com.github.steveash.synthrec.Constants.ORIGIN_CULTURE;
import static com.github.steveash.synthrec.Constants.PHONE;
import static com.github.steveash.synthrec.Constants.PHONE_PATTERN;
import static com.github.steveash.synthrec.Constants.SEX;
import static com.github.steveash.synthrec.Constants.SSN_PATTERN;

import java.util.Set;

import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.FactorGroup;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.gen.UnaryNode;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Provider for unary gennodes that dont need smoothing
 * @author Steve Ash
 */
@LazyComponent
public class UnaryGenNodeProvider implements GenNodeProvider {

    @Override
    public GenNode makeFor(String name, CountDag countDag
    ) {
        FactorGroup group = countDag.getFactorGroup(name);
        Preconditions.checkState(group.getFactorNonParentsName().size() == 1, "must be unary");
        Preconditions.checkState(group.getFactorParentsName().size() == 0, "must be unconditional");
        return new UnaryNode(group.getName(),
                SamplingTable.createFromMultinomial(group.makeUnconditionalUnaryCopy())
        );
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of(
                ORIGIN_CULTURE,
                SEX,
                PHONE_PATTERN,
                SSN_PATTERN,
                PHONE
        );
    }
}
