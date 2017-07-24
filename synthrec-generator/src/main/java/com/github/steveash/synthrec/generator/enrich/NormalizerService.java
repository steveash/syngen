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

package com.github.steveash.synthrec.generator.enrich;

import static com.github.steveash.synthrec.Constants.ADDRESS;
import static com.github.steveash.synthrec.Constants.ADDRESS_CITY;
import static com.github.steveash.synthrec.Constants.ADDRESS_STATE;
import static com.github.steveash.synthrec.Constants.ADDRESS_ZIP;
import static com.github.steveash.synthrec.Constants.DOB;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME;
import static com.github.steveash.synthrec.Constants.MIDDLE_NAME;
import static com.github.steveash.synthrec.Constants.PHONE;
import static com.github.steveash.synthrec.Constants.RACE;
import static com.github.steveash.synthrec.Constants.SEX;
import static com.github.steveash.synthrec.Constants.SSN;
import static com.github.steveash.synthrec.Constants.SUFFIX_NAME;
import static com.github.steveash.synthrec.canonical.Normalizers.interner;
import static com.github.steveash.synthrec.canonical.Normalizers.rawToStandard;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.canonical.Normalizers;
import com.github.steveash.synthrec.date.DateParser;
import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.phone.PhoneParser;
import com.github.steveash.synthrec.phone.PhoneParser.PhoneModel;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;

/**
 * Normalizers for input records
 * @author Steve Ash
 */
@LazyComponent
public class NormalizerService {

    public static final Function<String,String> STD_FUNC = (s) -> interner().intern(rawToStandard().normalize(s));
    public static final Function<String,String> NAME_FUNC = Names::normalizeIntern; //(s) -> interner().intern(standardToName().normalize(s));
    public static final Function<String,String> PHONE_FUNC = (s) -> {
        PhoneModel model = new PhoneParser().parse(s);
        if (model != null) {
            return model.toAreaExchangeNumberString();
        }
        return null;
    };
    private static final Function<String,String> SSN_FUNC = (s) -> {
        String digits = Normalizers.onlyDigits().normalize(s);
        if (digits.length() < 4) {
            return StringUtils.leftPad(digits, 4, '0');
        }
        if (digits.length() < 9) {
            return StringUtils.leftPad(digits, 9, '0');
        }
        if (digits.length() == 4 || digits.length() == 9) {
            return digits;
        }
        return null;
    };
    private static final Function<String,String> SEX_FUNC = (s) -> {
        String val = STD_FUNC.apply(s);
        switch (val) {
            case "F":
            case "FEMALE":
                return Constants.SEX_FEMALE;
            case "M":
            case "MALE":
                return Constants.SEX_MALE;
            case "U":
            case "UNKNOWN":
            case "OTHER":
                return Constants.SEX_UNKNOWN;
            default:
                return null;
        }
    };
    private static final CharMatcher ZIP_MATCHER = CharMatcher.anyOf("-0123456789");

    private static final Function<String,String> ZIP_FUNC = (s) -> {
        String zip = ZIP_MATCHER.retainFrom(s);
        int index = zip.indexOf('-');
        if (index >= 0) {
            zip = zip.substring(0, index);
        }
        if (zip.length() > 0) {
            zip = StringUtils.leftPad(zip, 5, "0");
            if (!zip.equals("00000")) {
                return zip;
            }
        }
        return null;
    };


    private static ImmutableListMultimap<String,Function<String,String>> DEFAULT = ImmutableListMultimap.<String,Function<String,String>>builder()
            .put(GIVEN_NAME, NAME_FUNC)
            .put(MIDDLE_NAME, NAME_FUNC)
            .put(FAMILY_NAME, NAME_FUNC)
            .put(SUFFIX_NAME, STD_FUNC)
            .put(ADDRESS, STD_FUNC)
            .put(ADDRESS_CITY, STD_FUNC)
            .put(ADDRESS_STATE, STD_FUNC)
            .put(ADDRESS_ZIP, ZIP_FUNC)
            .put(SSN, SSN_FUNC)
            .put(SEX, SEX_FUNC)
            .put(PHONE, STD_FUNC)
            .put(RACE, STD_FUNC)
            .build();

    private final ImmutableListMultimap<String,Function<String,String>> normalizers;

    @Autowired
    public NormalizerService(DateParser dateParser) {
        Builder<String,Function<String,String>> normalizers = ImmutableListMultimap.builder();
        normalizers.putAll(DEFAULT);
        normalizers.put(DOB, dateParser::normalizeToIsoOrEmpty);
        this.normalizers = normalizers.build();
    }

    public Record normalize(Record rec) {
        for (Entry<String, String> entry : rec.fields().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            rec.setNormal(key, interner().intern(value));
            if (isBlank(value)) {
                continue;
            }
            for (Function<String, String> func : normalizers.get(key)) {
                Preconditions.checkNotNull(func, "no normalizer registered for field ", key);
                value = func.apply(value);
                if (value != null) {
                    value = interner().intern(value);
                }
            }
            rec.setNormal(key, value);
        }
        return rec;
    }
}
