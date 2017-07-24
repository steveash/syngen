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

package com.github.steveash.synthrec.name;

/**
 * Represents a gender as an enum
 * @author Steve Ash
 */
public enum Gender {

    Male("M"), Female("F"), Both("B"), Unknown("U");

    public final String code;

    Gender(String code) {
        this.code = code;
    }

    public boolean contradicts(Gender that) {
        if (this == Male && that == Female) return true;
        if (this == Female && that == Male) return true;

        return false;
    }

    public boolean isPrecise() {
        return this == Male || this == Female;
    }

    public static Gender valueOfCode(String genderCode) {

        if (genderCode.equalsIgnoreCase(Male.code))
            return Male;
        if (genderCode.equalsIgnoreCase(Female.code))
            return Female;
        if (genderCode.equalsIgnoreCase(Both.code))
            return Both;
        if (genderCode.equalsIgnoreCase(Unknown.code))
            return Unknown;

        throw new IllegalArgumentException("Invalid gender code " + genderCode + " should be M,F,B,U");
    }

    /**
     * Given two genders -- this returns the result of combining them
     * @param a
     * @param b
     * @return
     */
    public static Gender combine(Gender a, Gender b) {
        if (a == Unknown) return b;
        if (b == Unknown) return a;
        if (a == Both || b == Both) return Both;
        if (a == b) return a;
        return Both;
    }
}
