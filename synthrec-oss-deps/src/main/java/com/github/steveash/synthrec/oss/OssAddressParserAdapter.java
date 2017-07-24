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

package com.github.steveash.synthrec.oss;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;

import com.github.steveash.synthrec.address.Address;
import com.github.steveash.synthrec.address.AddressSegment;
import com.github.steveash.synthrec.address.AddressTag;
import com.github.steveash.synthrec.address.RawAddressParser;
import com.github.steveash.synthrec.address.SimpleAddress;
import com.google.common.collect.Lists;
import com.skovalenko.geocoder.address_parser.ParsedUsAddress;
import com.skovalenko.geocoder.address_parser.UnparsedAddress;
import com.skovalenko.geocoder.address_parser.us.UsAddressParser;

/**
 * Adapter for the sample OSS Address Parser. This is not a state-of-the-art parser so I recommend using
 * SAS Dataflux or similar if you access to that
 * @author Steve Ash
 */
public class OssAddressParserAdapter implements RawAddressParser {

    private final UsAddressParser parser = new UsAddressParser();

    @Override
    public Address parse(String rawAddress) {
        ParsedUsAddress addr = parser.parse(new UnparsedAddress(rawAddress, null, null));
        List<AddressSegment> segs = Lists.newArrayList();
        append(segs, addr.getStreetNumber(), AddressTag.StreetNumber);
        append(segs, addr.getStreetPreDir(), AddressTag.PreDirection);
        append(segs, addr.getStreetName(), AddressTag.StreetName);
        append(segs, addr.getStreetPostDir(), AddressTag.PostDirection);
        append(segs, addr.getStreetType(), AddressTag.Designator);
        append(segs, addr.getSubUnitName(), AddressTag.AptTag);
        append(segs, addr.getSubUnitNumber(), AddressTag.AptObject);
        append(segs, addr.getCity(), AddressTag.City);
        append(segs, addr.getState(), AddressTag.State);
        append(segs, addr.getZip(), AddressTag.Zip5);
        return new SimpleAddress(segs);
    }

    private void append(List<AddressSegment> segs, String token, AddressTag tag) {
        if (isBlank(token)) {
            return;
        }
        segs.add(new AddressSegment(token, tag));
    }
}
