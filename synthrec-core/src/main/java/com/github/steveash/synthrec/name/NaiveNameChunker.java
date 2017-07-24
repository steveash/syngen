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
 * A name chunker that just finds the first family segment and assigns the split there;
 * It is highly recommended that you use a more sophisticated model to do this chunking; in the
 * paper we used a heuristic cost model that found an optimal split point
 * @author Steve Ash
 */
public class NaiveNameChunker implements NameChunker {

    @Override
    public ChunkedName chunk(PersonalName name) {
        boolean sawGiven = false;
        boolean sawSurname = false;
        for (int i = 0; i < name.size(); i++) {
            NameToken token = name.get(i);
            if (NamePart.isSurnameOrLineage(token.getPart())) {
                if (sawGiven) {
                    return new ChunkedName(i, NameSegment.Given, NameSegment.Family);
                }
                sawSurname = true;
            } else if (NamePart.isGivenOrMiddle(token.getPart())) {
                if (sawSurname) {
                    return new ChunkedName(i, NameSegment.Family, NameSegment.Given);
                }
                sawGiven = true;
            }
        }
        if (sawGiven && !sawSurname) {
            return new ChunkedName(-1, NameSegment.Given, NameSegment.Given);
        }
        if (sawSurname && !sawGiven) {
            return new ChunkedName(-1, NameSegment.Family, NameSegment.Family);
        }
        // default if you dont see anything is just to put it in given
        return new ChunkedName(-1, NameSegment.Given, NameSegment.Given);
    }
}
