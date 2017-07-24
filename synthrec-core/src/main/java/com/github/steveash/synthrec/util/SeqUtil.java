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

package com.github.steveash.synthrec.util;

import java.util.List;
import java.util.function.Predicate;

/**
 * Helper functions for doing operations on sequences
 * @author Steve Ash
 */
public class SeqUtil {

    public static <T> boolean anyBefore(List<T> seq, int myIndex, Predicate<T> test) {
        for (int i = 0; i < myIndex; i++) {
            if (test.test(seq.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean anyAfter(List<T> seq, int myIndex, Predicate<T> test) {
        for (int i = myIndex + 1; i < seq.size(); i++) {
            if (test.test(seq.get(i))) {
                return true;
            }
        }
        return false;
    }

}
