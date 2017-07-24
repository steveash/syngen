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

package com.github.steveash.synthrec.generator.reducer;

import static com.github.steveash.synthrec.string.PatternReducer.END_ESC;
import static com.github.steveash.synthrec.string.PatternReducer.START_ESC;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.steveash.synthrec.reducer.TaggedValue;
import com.github.steveash.synthrec.reducer.ValueReducer;
import com.github.steveash.synthrec.string.PatternReducer;
import com.google.common.base.Preconditions;

/**
 * A reducer that has some simple rules about what is an alphanumeric identifier that should be
 * reduced + has a dictionary of known good words that we _dont_ want to reduce
 * @author Steve Ash
 */
public class SimpleTokenReducer implements ValueReducer {

    // this tries to catch things that are all numbers, mostly numbers, or a couple of characters and a lot of
    // numbers mixed together -- i.e. patterns if identifiers that are obviously not real things to preserve
    private static final Pattern ID_PATTERNS = Pattern.compile(
            "(?:\\p{Punct}*\\d+\\p{Punct}*|\\p{Punct}*[A-Z]{1,2}[0-9]+\\p{Punct}*(?:[A-Z]{1,2})?\\p{Punct}*|\\p{Punct}*[0-9]+\\p{Punct}*[A-Z]{1,2}\\p{Punct}*(?:[0-9]+\\p{Punct}*)?|\\p{Punct}*[A-Z]{1,2}\\d+[A-Z]{1,2}\\d+\\p{Punct}*|\\p{Punct}[A-Z]{1,2}\\d|\\p{Punct}\\d[A-Z]{1,2})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WORD_PATTERNS1 = Pattern.compile(
            "\\p{Punct}*([A-Z]+)[0-9]+([A-Z]+)?\\p{Punct}*", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORD_PATTERNS2 = Pattern.compile(
            "\\p{Punct}*[0-9]+([A-Z]+)(?:[0-9]+)?\\p{Punct}*", Pattern.CASE_INSENSITIVE);

    public static final int MIN_RARE_COUNT = 100;

    private final Predicate<String> isKnownPublic;
    private final int minRareCount;

    public SimpleTokenReducer(int minRareCount, Predicate<String> isKnownPublic) {
        this.minRareCount = minRareCount;
        this.isKnownPublic = isKnownPublic;
    }

    public SimpleTokenReducer(int minRareCount) {
        this(minRareCount, s -> false);
    }

    public SimpleTokenReducer() {
        this(MIN_RARE_COUNT);
    }

    @Override
    public String reduceIfNecessary(String value, double empiricalCount) {
        if (TaggedValue.isTagged(value)) {
            return value;
        }
        if (isBlank(value)) {
            return value;
        }
        // these rules always transform and never try to preserve values
        if (matchesRules(value)) {
            return xform(value);
        }
        String maybe = maybeEscape(WORD_PATTERNS1, value);
        if (maybe != null) {
            return xform(maybe);
        }
        String maybe2 = maybeEscape(WORD_PATTERNS2, value);
        if (maybe2 != null) {
            return xform(maybe2);
        }

        // now start applying rules about the count, so rare values get somewhat aggressive and
        // really rare values get really aggressive rules
        if (empiricalCount < 0 || empiricalCount > minRareCount) {
            return value;
        }
        // infrequent 1 or 2 character
        if (value.length() <= 2) {
            return xform(value);
        }
        if (value.length() == 3 && !isKnownPublic.test(value)) {
            return xform(value);
        }
        return value;
    }

    // if you can/should escape this then return the escaped value, otherwise null
    private String maybeEscape(Pattern patt, String value) {
        Matcher matcher = patt.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        String first = matcher.group(1);
        Preconditions.checkState(isNotBlank(first), "shouldnt be blank");
        int offset = 0; // if we modify then the original value string will be offset by the escaped chars
        if (isKnownPublic.test(first)) {
            value = value.substring(0, matcher.start(1)) + START_ESC + first + END_ESC +
                    value.substring(matcher.end(1));
            offset += START_ESC.length() + END_ESC.length();
        } else if (first.length() > 1) {
            return null; // we matched a non public value thats longer than a character, not going to reduce
        }
        if (matcher.groupCount() > 1 && isNotBlank(matcher.group(2))) {
            String second = matcher.group(2);
            if (isKnownPublic.test(second)) {
                value = value.substring(0, offset + matcher.start(2)) + START_ESC + second + END_ESC +
                        value.substring(offset + matcher.end(2));
                offset += START_ESC.length() + END_ESC.length();
            } else if (second.length() > 1) {
                // second matched but its too long
                return null;
            }
        }
        // if we're here then we have escaped what we want AND we're eligible for a transform
        return value;
    }

    private String xform(String value) {
        return PatternReducer.replaceAndTag(value);
    }

    public static boolean matchesRules(String input) {
        return ID_PATTERNS.matcher(input).matches();
    }
}
