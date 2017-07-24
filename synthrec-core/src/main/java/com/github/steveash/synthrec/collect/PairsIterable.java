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

package com.github.steveash.synthrec.collect;

import static com.google.common.collect.Iterators.advance;
import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficient;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;

/**
 * Returns the n Choose 2 pairs from an iterable excluding self-pairs (so its not really
 * a cartesian product)
 */
public class PairsIterable<T> implements Iterable<Pair<T, T>> {

    public static <T> PairsIterable<T> of(Iterable<? extends T> source) {
        return new PairsIterable<T>(source);
    }

    private final Iterable<? extends T> source;
    private final long totalCount;

    private PairsIterable(Iterable<? extends T> source) {
        this.source = source;
        this.totalCount = calculateTotalCount(source);
    }

    private static <T> long calculateTotalCount(Iterable<T> source) {
        if (source instanceof Collection) {
            int inputSize = ((Collection)source).size();
            if (inputSize >= 2)
                return binomialCoefficient(inputSize, 2);

            return 0;
        }
        return -1;
    }

    public long getTotalCount() {
        return totalCount;
    }

    @Override
    public Iterator<Pair<T, T>> iterator() {
        return new AbstractIterator<Pair<T, T>>() {

            private Iterator<? extends T> outer = source.iterator();
            private Iterator<? extends T> inner = source.iterator();

            private int nextInnerSkip = 1; // start at one as no self-pairs
            private T lastOuter;

            {
                // setup for the initial state
                if (outer.hasNext()) {
                    lastOuter = outer.next();
                    positionInnerIterator();
                }
            }

            @Override
            protected Pair<T, T> computeNext() {
                if (!inner.hasNext()) {
                    if (!outer.hasNext()) {
                        // this can only happen in the empty input case or if we enhance this to do self-pairs
                        return endOfData();
                    }

                    lastOuter = outer.next();
                    inner = source.iterator();
                    positionInnerIterator();
                    // because we don't do self refs we will actually skip ahead to the end of the inner before
                    // running out in the outer
                    if (!inner.hasNext()) {
                        Preconditions.checkState(!outer.hasNext());
                        return endOfData();
                    }
                }
                T next = inner.next();
                Pair<T, T> result = Pair.of(lastOuter, next);
                return result;
            }

            private void positionInnerIterator() {
                // precondition: inner iterator is right before first element
                int actuallySkipped = advance(inner, this.nextInnerSkip);
                Preconditions.checkState(actuallySkipped == this.nextInnerSkip);
                nextInnerSkip += 1;
            }
        };
    }
}
