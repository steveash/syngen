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

package com.github.steveash.synthrec.phone;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Simple regex based phone parser
 * @author Steve Ash
 */
public class PhoneParser {

    public static class PhoneModel {
        private final String countryCode;
        private final String areaCode;
        private final String exchange;
        private final String number;
        private final String extension;

        public PhoneModel(String countryCode, String areaCode, String exchange, String number, String extension) {
            this.countryCode = countryCode;
            this.areaCode = areaCode;
            this.exchange = exchange;
            this.number = number;
            this.extension = extension;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getAreaCode() {
            return areaCode;
        }

        public String getExchange() {
            return exchange;
        }

        public String getNumber() {
            return number;
        }

        public String getExtension() {
            return extension;
        }

        /**
         * Returns xxx-xxx-xxxx zero padding where necessary
         * @return
         */
        public String toAreaExchangeNumberString() {
            return areaCode + "-" + exchange + "-" + number;
        }

        public String toCanonicalString() {
            StringBuilder sb = new StringBuilder();
            if (countryCode != null) {
                sb.append("+").append(countryCode).append("-");
            }
            sb.append(areaCode).append("-");
            sb.append(exchange).append("-");
            sb.append(number);
            if (extension != null) {
                sb.append(", ext. ").append(extension);
            }
            return sb.toString();
        }
    }

    private static final CharMatcher ONLY_GOOD = CharMatcher.digit().or(CharMatcher.anyOf(" .-()x"));

    @Nullable
    public PhoneModel parse(String raw) {
        if (isBlank(raw)) return null;

        PhoneModel maybe = tryMatch(raw);
        if (maybe != null) return maybe;
        // try a second time only retaining good stuff; less accurate but more permissive
        String goods = ONLY_GOOD.retainFrom(raw);
        return tryMatch(goods);
    }

    private PhoneModel tryMatch(String raw) {
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(raw);
            if (!matcher.matches()) {
                continue;
            }
            String c = null;
            try {
                c = Strings.emptyToNull(matcher.group("c"));
            } catch (IllegalArgumentException e) {
                // no c group
            }
            String a = format(3, matcher.group("a"));
            String e = format(3, matcher.group("e"));
            String n = format(4, matcher.group("n"));
            String x = Strings.emptyToNull(matcher.group("x"));
            return new PhoneModel(c, a, e, n, x);
        }
        return null;
    }

    private String format(int length, String raw) {
        if (raw == null) {
            raw = "";
        }
        return left(leftPad(raw, length, '0'), length);
    }

    private static final List<Pattern> PATTERNS = ImmutableList.<Pattern>builder()
            .add(Pattern.compile( "\\s*(?:\\+?(?<c>\\d{1,3}))?[ -.(]*(?<a>\\d{3})[ -.)]*(?<e>\\d{3})[ -.]*(?<n>\\d{4})[ ,;]*(?:(?:ex|ext|extn|extension|,\\s*|x+)[ :.,-]*(?<x>\\d{1,6})?\\s*)?", Pattern.CASE_INSENSITIVE))
            .add(Pattern.compile( "\\s*(?:(?<a>\\d{3})[ -.)]*)?(?<e>\\d{3})[ -.]*(?<n>\\d{4})[ ,;]*(?:(?:ex|ext|extn|extension|,\\s*|x+)[ :.,-]*(?<x>\\d{1,6})?\\s*)?", Pattern.CASE_INSENSITIVE))
            .build();
}
