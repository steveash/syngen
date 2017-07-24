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

package com.github.steveash.synthrec.address;

import java.util.Arrays;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * The semantic tags of the address model.
 * @author Steve Ash
 */
public enum AddressTag {

    StreetNumber("STREET_NO"),
    PreDirection("PREDIR"),
    StreetName("STREET_NAME"),
    PostDirection("POSTDIR"),
    Designator("DESIGNATOR"),
    CoTag("C/O_TAG"),
    CoObject("C/O_OBJ"),
    AptTag("APT/BOX_TAG"),
    AptObject("APT/BOX_OBJ"),
    PoBoxTag("POBOX_TAG"),
    PoBoxObject("POBOX_OBJ"),
    RrTag("RR_TAG"),
    RrObject("RR_OBJ"),
    RrBoxTag("RRBOX_TAG"),
    RrBoxObject("RRBOX_OBJ"),
    HighwayTag("HWY_TAG"),
    HighwayObject("HWY_OBJ"),
    City("CITY"),
    State("STATE"),
    Zip5("ZIP5"),
    Zip4("ZIP4"),
    Zip9("ZIP9"),
    Country("COUNTRY"),
    Recipient("RECIPIENT");

    public static final Map<String,AddressTag> TagTextToTag = Maps.uniqueIndex(Arrays.asList(AddressTag.values()),
            new Function<AddressTag, String>() {
        @Override
        public String apply(AddressTag input) {
            return input.tagText;
        }
    });

    public static final int getCount() {
        return AddressTag.values().length;
    }

    public final String tagText;

    private AddressTag(String tagText) {

        this.tagText = tagText;
    }
}
