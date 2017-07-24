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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.commons.convert.MiscConverters.StringToSimpleDateFormat;

/**
 * Like a string builder but for working with tokens; automatically inserts spaces between tokens for you
 *
 * @author Steve Ash
 */
public class TokenStringBuilder {

    private static final String DEFAULT_SEP = " ";
    private final StringBuilder sb = new StringBuilder();
    private final String sep;
    private boolean isEmpty = true;

    public TokenStringBuilder() {
        this(DEFAULT_SEP);
    }

    public TokenStringBuilder(String separator) {
        this.sep = separator;
    }

    public TokenStringBuilder append(String token) {
        if (isNotBlank(token)) {
            if (!isEmpty) {
                sb.append(sep);
            }
            isEmpty = false;
            sb.append(token);
        }
        return this;
    }

    public TokenStringBuilder clear() {
        sb.delete(0, sb.length());
        isEmpty = true;
        return this;
    }

    public int length() {
        return sb.length();
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public String toString() {
        if (isEmpty()) {
            return "";
        }
        return sb.toString();
    }
}
