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

package com.github.steveash.synthrec.name.culture;

import com.github.steveash.synthrec.canonical.NormalToken;
import com.github.steveash.synthrec.canonical.SimpleNormalToken;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.stat.Multinomial;

/**
 * A detector that can return a multinomial of cultures given a name token or multiple tokens
 * @author Steve Ash
 */
public interface CultureDetector {

    default Multinomial<String> detectSingleToken(String rawToken) {
        return detectSingleToken(new SimpleNormalToken(rawToken, Names.normalize(rawToken)));
    }

    Multinomial<String> detectSingleToken(NormalToken name);
}
