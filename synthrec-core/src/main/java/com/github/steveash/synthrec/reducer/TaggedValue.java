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

package com.github.steveash.synthrec.reducer;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

/**
 * When we do special things to values to transform them (like turn them into patterns, we "tag" them with
 * a moniker that indicates who did the transformation). This just records that in a single place
 * All tags are structured like <!TAG_NAME!>=
 * @author Steve Ash
 */
public class TaggedValue {
    private static final Pattern TAG_PATTERN = Pattern.compile("<![A-Z_]+!>=.*");

    public static boolean isTagged(String candidate) {
        return candidate.startsWith("<!") && TAG_PATTERN.matcher(candidate).matches();
    }

    public static boolean isTaggedWith(String tag, String candidate) {
        return candidate.startsWith(tag);
    }

    public static String unTag(String candidate) {
        int idx = candidate.indexOf("!>=");
        Preconditions.checkState(idx > 0, "value doesnt seem to be tagged", candidate);
        return candidate.substring(idx + 3);
    }

    public static String unTag(String tag, String candidate) {
        return candidate.substring(tag.length());
    }

    public static String tag(String tag, String value) {
        return tag + value;
    }
}
