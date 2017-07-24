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

package com.github.steveash.synthrec.generator.feature;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.address.Address;
import com.github.steveash.synthrec.address.RawAddressParser;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;

/**
 * @author Steve Ash
 */
@LazyComponent
public class AddressParserFeature extends SingleFeatureComputer {

    public static final FeatureKey<Address> ADDRESS_PARSED_FEATURE = new FeatureKey<>(Constants.ADDRESS_PARSED, Address.class);

    private final RawAddressParser rawAddressParser;

    @Autowired
    public AddressParserFeature(RawAddressParser rawAddressParser) {
        super(ADDRESS_PARSED_FEATURE);
        this.rawAddressParser = rawAddressParser;
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        String fullAddress = record.getNormal(Constants.ADDRESS, null);
        if (fullAddress == null) {
            return;
        }
        Address address = rawAddressParser.parse(fullAddress);
        if (address.isEmpty()) {
            return;
        }
        sink.setFeature(ADDRESS_PARSED_FEATURE, address);
    }
}
