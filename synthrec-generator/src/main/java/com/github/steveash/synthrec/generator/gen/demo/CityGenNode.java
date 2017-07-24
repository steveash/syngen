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

import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.InOutGenNode;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.socio.ZipData;
import com.github.steveash.synthrec.socio.ZipDataLookup;
import com.google.common.base.Optional;

/**
 * @author Steve Ash
 */
@LazyComponent
public class CityGenNode extends InOutGenNode {

    @Resource private ZipDataLookup zipDataLookup;

    public CityGenNode() {
        super(Constants.ADDRESS_ZIP, Constants.ADDRESS_CITY);
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context
    ) {
        String zip = (String) assignment.get(Constants.ADDRESS_ZIP);
        Optional<ZipData> info = zipDataLookup.apply(zip);
        if (!info.isPresent()) {
            return false;
        }
        String city = info.get().getCity();
        assignment.put(Constants.ADDRESS_CITY, city.toUpperCase().trim());
        return true;
    }
}
