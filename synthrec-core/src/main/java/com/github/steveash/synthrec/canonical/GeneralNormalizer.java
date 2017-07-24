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

import static com.google.common.base.CharMatcher.ascii;
import static com.google.common.base.CharMatcher.javaIsoControl;

import com.google.common.base.CharMatcher;

/**
 * @author Steve Ash
 */
public class GeneralNormalizer implements StringNormalizer {

    static final GeneralNormalizer INSTANCE = new GeneralNormalizer();

    private final StringNormalizer replaceDiacritics = DiacriticNormalizer.DIA_TO_LOWER_NORMAL;
    private final CharMatcher lettersDigitsAndPunc = ascii().and(javaIsoControl().negate()).precomputed();
    private final CharMatcher trimChars = CharMatcher.invisible().or(CharMatcher.anyOf("%"));
    private final CharMatcher replaceWithApostrophe = CharMatcher.anyOf("`~^").precomputed();
    private final CharMatcher replaceWithSpace = CharMatcher.anyOf("|").precomputed();

    /**
     * {@inheritDoc}
     */
    @Override
    public String normalize(String input) {
        String work = input;
        work = replaceDiacritics.normalize(work);
        work = lettersDigitsAndPunc.retainFrom(work);
        work = replaceWithApostrophe.replaceFrom(work, '\'');
        work = replaceWithSpace.replaceFrom(work, ' ');
        work = trimChars.trimAndCollapseFrom(work, ' ');
        return work.toUpperCase();
    }
}
