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

package com.github.steveash.synthrec.generator.deident;

import javax.annotation.Resource;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.GivenNameLookup;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.name.SurnameLookup;
import com.github.steveash.synthrec.name.NameStopWords;
import com.github.steveash.synthrec.string.PatternReducer;
import com.github.steveash.synthrec.string.StringBinner;

/**
 * Common rules for any name token on what is public domain or not
 * @author Steve Ash
 */
@LazyComponent
public class CommonNamePublicRule {

    @Resource private SurnameLookup surnameLookup;  // agg of all public names we know about
    @Resource private GivenNameLookup givenNameLookup;
    @Resource private NameStopWords nameStopWords;

    public boolean isPublicDomain(String vocabToken) {
        if (StringBinner.isTagged(vocabToken) ||
                PatternReducer.isTagged(vocabToken) ||
                Constants.MISSING.equals(vocabToken))
        {
            return true;
        }
        String normalToken = Names.normalize(vocabToken);
        return normalToken.length() == 1 ||
                nameStopWords.isStopword(normalToken) ||
                givenNameLookup.isPublicName(normalToken) ||
                surnameLookup.isPublicName(normalToken);
    }
}
