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

package com.github.steveash.synthrec.nametag;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Used to match multi-token seqeuences (like honoriffics). Multimap of root token (since thats really selective) ->
 * rest of the token list
 * @author Steve Ash
 */
public class MultiMatcher {

    public interface TokenSeq {
        String get(int t);
        int size();
    }

    public static class ListTokenSeq<T> implements TokenSeq {

        public static TokenSeq make(String... elements) {
            return make(Arrays.asList(elements));
        }

        public static TokenSeq make(List<String> toWrap) {
            return new ListTokenSeq<>(toWrap, Function.identity());
        }

        public static <T> TokenSeq make(List<T> toWrap, Function<T,String> xform) {
            return new ListTokenSeq<>(toWrap, xform);
        }

        private final List<T> list;
        private final Function<T, String> converter;

        public ListTokenSeq(List<T> list, Function<T, String> xform) {
            this.list = list;
            this.converter = xform;
        }

        @Override
        public String get(int t) {
            return converter.apply(list.get(t));
        }

        @Override
        public int size() {
            return list.size();
        }
    }

    public static MultiMatcher makeFrom(Iterable<? extends List<String>> lines, Function<String,String> normalizer) {
        Builder<String, ImmutableList<String>> builder = ImmutableMultimap.builder();
        for (List<String> line : lines) {
            if (line.isEmpty()) continue;
            ImmutableList<String> others = line.subList(1, line.size()).stream()
                    .map(normalizer)
                    .collect(Guavate.toImmutableList());
            builder.put(normalizer.apply(line.get(0)), others);
        }
        return new MultiMatcher(builder.build(), normalizer);
    }

    private final ImmutableMultimap<String, ImmutableList<String>> matches;
    private final Function<String, String> singleNormalizer;
    private final ImmutableSet<String> singles;

    MultiMatcher(ImmutableMultimap<String, ImmutableList<String>> matches, Function<String,String> singleNormalizer) {
        this.matches = matches;
        this.singleNormalizer = singleNormalizer;
        this.singles = matches.entries().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Entry::getKey)
                .collect(Guavate.toImmutableSet());
    }

    public ImmutableSet<String> getSingles() {
        return singles;
    }

    public Iterable<String> allTokens() {
        Iterable<String> ible = matches.values().stream().flatMap(Collection::stream)::iterator;
        return Iterables.concat(singles, matches.keySet(), ible);
    }

    public boolean singleMatch(String normalizedToCheck) {
        return singles.contains(normalizedToCheck);
    }

    public int prefixMatches(TokenSeq candidate) {
        return prefixMatches(candidate, 0);
    }

    public int prefixMatches(TokenSeq candidate, int startingAt) {
        if (candidate.size() - startingAt < 1) {
            return -1;
        }
        ImmutableCollection<ImmutableList<String>> maybe = matches.get(candidate.get(startingAt));
        if (maybe == null) {
            return -1;
        }
        int best = -1;
        for (ImmutableList<String> test : maybe) {
            best = Math.max(best, prefixMatchTarget(candidate, startingAt + 1, test));
        }
        if (best >= 0) {
            return best + 1;
        }
        return best;
    }

    private int prefixMatchTarget(TokenSeq candidate, int candidateStartIndex, ImmutableList<String> targetToMatch) {
        int maxCandidateToCheck = candidate.size() - candidateStartIndex;
        if (maxCandidateToCheck < targetToMatch.size()) {
            return -1; // cant match if the candidate is smaller than target
        }
        int i = candidateStartIndex; // candidate start index
        // need to match the whole target
        for (int j = 0; j < targetToMatch.size(); j++) {
            if (!candidate.get(i).equalsIgnoreCase(targetToMatch.get(j))) {
                return -1;
            }
            i += 1;
        }
        return targetToMatch.size();
    }
}
