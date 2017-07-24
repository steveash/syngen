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

import java.util.concurrent.atomic.LongAdder;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.collect.Vocabulary;

/**
 * This is a deidentifier that goes through a vocab and replaces every entry by generating
 * a pattern that represents the structure of the data
 * @author Steve Ash
 */
public class RemapDeidentifier<T> implements VocabDeidentifier<T> {
    private static final Logger log = LoggerFactory.getLogger(RemapDeidentifier.class);

    public interface Remapper<T> {
        T remap(T input, ToDoubleFunction<T> countForVocab);
    }

    private final Remapper<T> remapper;

    public RemapDeidentifier(Remapper<T> remapper) {this.remapper = remapper;}

    @Override
    public void deidentify(Vocabulary<T> vocab, ToDoubleFunction<T> countForVocab, Observer observer) {
        LongAdder replaceCount = new LongAdder();
        LongAdder skipCount = new LongAdder();
        // we're going through the vocab as it exists before deident; we might
        // add more entries but those would all be replaced entries from the
        // remapper so no need to re-deidenty them
        IntStream.range(1, vocab.nextIndex()).parallel().forEach( index -> {
            T maybe = vocab.getForIndexNoResolve(index);
            if (maybe == null) {
                return;
            }
            T replaceValue = remapper.remap(maybe, countForVocab);
            if (replaceValue == null || replaceValue.equals(maybe)) {
                skipCount.increment();
                return;
            }
            replaceCount.increment();
            vocab.updateIndexValue(index, replaceValue);
        });

        log.info("Remapper remapped {} items and skipped {}", replaceCount.sum(), skipCount.sum());
    }
}
