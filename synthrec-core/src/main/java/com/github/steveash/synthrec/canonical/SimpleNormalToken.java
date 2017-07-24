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

package com.github.steveash.synthrec.canonical;

/**
 * @author Steve Ash
 */
public class SimpleNormalToken implements NormalToken {

    private final String originalToken;
    private final String normalToken;

    public SimpleNormalToken(String originalToken, String normalToken) {
        this.originalToken = originalToken;
        this.normalToken = normalToken;
    }

    @Override
    public String toString() {
        return "SimpleNameInput{" +
                "originalToken='" + originalToken + '\'' +
                ", normalToken='" + normalToken + '\'' +
                '}';
    }

    @Override
    public String getOriginalToken() {
        return originalToken;
    }

    @Override
    public String getNormalToken() {
        return normalToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleNormalToken that = (SimpleNormalToken) o;

        return originalToken != null ? originalToken.equals(that.originalToken) : that.originalToken == null;
    }

    @Override
    public int hashCode() {
        return originalToken != null ? originalToken.hashCode() : 0;
    }
}
