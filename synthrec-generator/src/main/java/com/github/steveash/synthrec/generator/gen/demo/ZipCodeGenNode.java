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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.InOutGenNode;
import com.github.steveash.synthrec.generator.feature.CityBinFeature.CityBin;
import com.github.steveash.synthrec.generator.prior.ZipSmoother;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Samples a zip code given the city bin and the
 * @author Steve Ash
 */
@LazyComponent
public class ZipCodeGenNode extends InOutGenNode {

    private final Map<Pair<String, CityBin>, SamplingTable<String>> stateBinToZip;

    @Autowired
    public ZipCodeGenNode(ZipSmoother zipSmoother) {
        super(Constants.ADDRESS_STATE,
                Constants.ADDRESS_CITY_BIN,
                Constants.ADDRESS_ZIP
        );
        stateBinToZip = zipSmoother.makeZipConditional();
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        String state = (String) assignment.get(Constants.ADDRESS_STATE);
        CityBin cityBin = (CityBin) assignment.get(Constants.ADDRESS_CITY_BIN);
        SamplingTable<String> zipSamples = stateBinToZip.get(Pair.of(state, cityBin));
        if (zipSamples == null) {
            return false;
        }
        String zip = zipSamples.sampleWeighted(rand);
        assignment.put(Constants.ADDRESS_ZIP, zip);
        return true;
    }
}
