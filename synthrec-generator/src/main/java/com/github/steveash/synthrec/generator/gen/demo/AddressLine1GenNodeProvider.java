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

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.address.AddressStopWords;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.FactorStats;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.prior.AddressCounts;
import com.github.steveash.synthrec.generator.prior.AddressStructSmoother;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.string.PatternExpander;
import com.google.common.collect.ImmutableSet;

/**
 * Provider to build gen nodes that can turn an address structure into a sample value
 * @author Steve Ash
 */
@LazyComponent
public class AddressLine1GenNodeProvider implements GenNodeProvider {

    @Resource private AddressStructSmoother smoother;
    @Resource private AddressStopWords addressStopWords;

    @Override
    public GenNode makeFor(String name, CountDag countDag) {
        FactorStats stats = countDag.getFactorStats(Constants.ADDRESS_STREET_STRUCT);
        Map<String, MutableMultinomial<Object>> map = stats.makeSubfieldUnaryCopy();
        PatternExpander expander = new PatternExpander(3, AddressCounts.loadAllWords(addressStopWords));
        Map<String, Sampler<?>> sampler = smoother.smooth(map);
        return new FieldSketchGenNode(Constants.ADDRESS_STREET_STRUCT,
                name,
                sampler::get,
                expander
        );
    }

    @Override
    public Set<String> providesForNames() {
        return ImmutableSet.of(Constants.ADDRESS_LINE1);
    }
}
