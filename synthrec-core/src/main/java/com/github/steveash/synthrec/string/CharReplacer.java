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

import javax.annotation.CheckReturnValue;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.stat.RandUtil;
import com.google.common.base.CharMatcher;

/**
 * @author Steve Ash
 */
public class CharReplacer {

    private static final CharMatcher ALPHA = CharMatcher.inRange('a', 'z')
            .or(CharMatcher.inRange('A', 'Z'))
            .precomputed();

    /**
     * Takes a string and any consecutive chars >= k are replaced with random chars
     * @param input
     * @param minToReplace
     * @return
     */
    public static String replaceConsecutiveAlpha(RandomGenerator rand, String input, int minToReplace) {
        StringBuilder sb = null;
        int spanStart = -1;
        boolean inSpan = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (ALPHA.matches(c)) {
                if (inSpan) {
                    // we're already scanning a potential victim substring so keep going
                } else {
                    assert spanStart == -1;
                    inSpan = true;
                    spanStart = i;
                }
            } else {
                if (inSpan) {
                    // we just cross a boundary where we might need to replace stuff
                    int span = i - spanStart;
                    if (span >= minToReplace) {
                        sb = dumpSpan(rand, sb, input, spanStart, i);
                    } else {
                        dumpRetain(sb, input, spanStart, i);
                    }
                    inSpan = false;
                    spanStart = -1;
                    dumpRetain(sb, c);
                } else {
                    // we aren't matching and we aren't in a scan so update because im good
                    assert spanStart == -1;
                    dumpRetain(sb, c);
                }
            }
        }
        if (inSpan) {
            assert spanStart >= 0;
            // dump the last seg
            int span = input.length() - spanStart;
            if (span >= minToReplace) {
                sb = dumpSpan(rand, sb, input, spanStart, input.length());
            } else {
                dumpRetain(sb, input, spanStart, input.length());
            }
        }
        if (sb != null) {
            return sb.toString();
        }
        return input;
    }

    private static void dumpRetain(StringBuilder sb, String input, int dumpStart, int endExcl) {
        if (sb == null) {
            return;
        }
        sb.append(input.substring(dumpStart, endExcl));
    }

    private static void dumpRetain(StringBuilder sb, char c) {
        if (sb == null) {
            return;
        }
        sb.append(c);
    }

    @CheckReturnValue
    private static StringBuilder dumpSpan(RandomGenerator rand,
            StringBuilder sb,
            String input,
            int spanStart,
            int endExcl
    ) {
        if (sb == null) {
            sb = new StringBuilder(input.length());
            if (spanStart > 0) {
                sb.append(input.substring(0, spanStart));
            }
        }
        sb.append(RandUtil.randomAlpha(rand, endExcl - spanStart));
        return sb;
    }
}
