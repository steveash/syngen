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

package com.github.steveash.synthrec.name.gender;

import com.github.steveash.synthrec.canonical.NormalToken;
import com.github.steveash.synthrec.canonical.SimpleNormalToken;
import com.github.steveash.synthrec.name.Gender;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.stat.Multinomial;

/**
 * Knows how to determine the probability of the gender of a given-name token
 * @author Steve Ash
 */
public interface GenderTagger {

    Multinomial<Gender> predictGender(NormalToken input);

    Gender tagGender(NormalToken input);

    default Gender tagGender(String rawString) {
        return tagGender(new SimpleNormalToken(rawString, Names.normalize(rawString)));
    }

    default Multinomial<Gender> predictGender(String rawString) {
        return predictGender(new SimpleNormalToken(rawString, Names.normalize(rawString)));
    }
}
