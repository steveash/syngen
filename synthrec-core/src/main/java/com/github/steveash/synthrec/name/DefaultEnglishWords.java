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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * @author Steve Ash
 */
public class DefaultEnglishWords implements EnglishWords {

    public static DefaultEnglishWords fromClasspath() {
        Set<String> engs = Sets.newHashSetWithExpectedSize(600);
        Names.loadNameTextData("words/google-10000-english-usa-no-swears-short.clob", 600).forEach(engs::add);
        Names.loadNameTextData("words/google-10000-english-usa-no-swears-medium.clob", 600).forEach(engs::add);
        return new DefaultEnglishWords(engs);
    }

    private final ImmutableSet<String> freqEnglish;

    public DefaultEnglishWords(Set<String> freqEnglish) {this.freqEnglish = ImmutableSet.copyOf(freqEnglish);}

    @Override
    public ImmutableSet<String> getFreqEnglish() {
        return freqEnglish;
    }
}
