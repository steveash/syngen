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

package com.github.steveash.synthrec.deident;

import java.util.function.ToDoubleFunction;

import com.github.steveash.synthrec.collect.Vocabulary;

/**
 * A strategy to make a distribution anonymous under some model on anonymity
 * @author Steve Ash
 */
public interface VocabDeidentifier<I> {

    Observer NULL_OBSERVER = new Observer() { };

    void deidentify(Vocabulary<I> vocab, ToDoubleFunction<I> countForVocab, Observer observer);

    // this is a little bit of a wart; this is the union of all observer needs for all deident impls
    interface Observer {

        default void onBlockingReplace(Object sensitive, Object replacement) {}
        default void onSampleReplace(Object sensitive, Object replacement){}
    }
}
