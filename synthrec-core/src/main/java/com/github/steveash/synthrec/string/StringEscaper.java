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

package com.github.steveash.synthrec.string;

import static com.github.steveash.synthrec.string.PatternReducer.END_ESC;
import static com.github.steveash.synthrec.string.PatternReducer.START_ESC;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

/**
 * Takes a list of tokens that are valid to escape and escapes them with
 * @see PatternReducer#START_ESC
 * @see PatternReducer#END_ESC
 * @author Steve Ash
 */
public class StringEscaper {

    private final Pattern pattern;

    public StringEscaper(String... escapes) {
        this(ImmutableSet.copyOf(escapes));
    }

    public StringEscaper(Set<String> escapes) {
        String opts = escapes.stream().map(Pattern::quote).collect(Collectors.joining("|"));
        this.pattern = Pattern.compile("(" + opts + ")", Pattern.CASE_INSENSITIVE);
    }

    public String escape(String input) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer out = null;
        while (matcher.find()) {
            if (out == null) {
                out = new StringBuffer(input.length() + START_ESC.length() + END_ESC.length());
            }
            String group = matcher.group(1);
            matcher.appendReplacement(out, START_ESC + group + END_ESC);
        }
        if (out == null) {
            return input; // nothing to replace
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
