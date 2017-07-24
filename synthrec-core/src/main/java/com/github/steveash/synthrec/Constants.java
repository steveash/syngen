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

package com.github.steveash.synthrec;

import com.google.common.collect.ImmutableList;

/**
 * @author Steve Ash
 */
public class Constants {

    public static final String MISSING = "<<!MISSING!>>";

    // fields as segmented in input/output records
    public static final String PREFIX_NAME = "name-prefix";
    public static final String GIVEN_NAME = "name-given";
    public static final String MIDDLE_NAME = "name-middle";
    public static final String FAMILY_NAME = "name-family";
    public static final String SUFFIX_NAME = "name-suffix";
    public static final String GIVEN_NAMEISH = "name-given-like"; // just identifying given name tokens
    public static final String FAMILY_NAMEISH = "name-family-like";
    public static final String ADDRESS = "address-full"; // full address in one string
    public static final String ADDRESS_STREET_NO = "address-street-no";
    public static final String ADDRESS_STREET = "address-street";
    public static final String ADDRESS_CITY = "address-city";
    public static final String ADDRESS_CITY_BIN = "address-city-bin"; // binned the city into 3 bins based on size
    public static final String ADDRESS_STATE = "address-state";
    public static final String ADDRESS_ZIP = "address-zip";
    public static final String ADDRESS_APT = "address-apt";
    public static final String DOB = "dob";
    public static final String DOB_PARSED = "dob-parsed";
    public static final String DOB_PATTERN = "dob-pattern";
    public static final String BIRTH_STATE = "birth-state";
    public static final String SSN = "ssn";
    public static final String SSN_PATTERN = "ssn-pattern";
    public static final String SEX = "sex";
    public static final String AGE_YEARS = "age-years";
    public static final String PHONE = "phone";
    public static final String PHONE_PATTERN = "phone-pattern";
    public static final String RACE = "race";

    // fields derived or sub-fields
    public static final String ORIGIN_CULTURE = "culture";
    public static final String GIVEN_NAME_CULTURE = "given-name-culture";
    public static final String GIVEN_FAMILY_JOINT = "given-family-joint";
    public static final String FAMILY_NAME_CULTURE = "family-name-culture";
    public static final String GIVEN_NAME_STRUCT = "given-name-structure";
    public static final String FAMILY_NAME_STRUCT = "family-name-structure";
    public static final String NAME_PARSED = "nams-parsed";
    public static final String ADDRESS_PARSED = "address-parsed";
    public static final String ADDRESS_STREET_STRUCT = "address-street-structure";
    public static final String ADDRESS_LINE1 = "address-line1";

    public static final ImmutableList<String> NAME_SEGMENTS = ImmutableList.of(GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME );
    public static final ImmutableList<String> NAME_SEGMENTS_SUFFIX = ImmutableList.of(GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME, SUFFIX_NAME);
    public static final ImmutableList<String> ALL_NAME_SEGMENTS = ImmutableList.of(PREFIX_NAME, GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME, SUFFIX_NAME);

    public static final String SEX_FEMALE = "F";
    public static final String SEX_MALE = "M";
    public static final String SEX_UNKNOWN = "U";
}
