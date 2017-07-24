
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

package com.github.steveash.synthrec.data;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

/**
 *
 * @author Steve Ash
 */
public class TranslationTable {
    private static final Logger log = LoggerFactory.getLogger(TranslationTable.class);


    public static final Function<TranslationTable, Iterable<Entry<String,String>>> SelectAllTranslations =
            TranslationTable::getAllTranslations;


    private static final Splitter commaSplit = Splitter.on(',').trimResults().limit(2);

    public static TranslationTable makeFromClasspathResource(String resourceName) {
        return ReadWrite.rawLinesFrom(resourceName, TranslationTable::makeFromCsvLines);
    }

    private static TranslationTable makeFromCsvLines(List<String> lines) {
        Builder<String, String> builder = ImmutableMap.builder();
        for (String line : lines) {
            if (isBlank(line))
                continue;

            if (line.startsWith("#"))
                continue;

            try {
                Iterator<String> split = commaSplit.split(line).iterator();
                Preconditions.checkState(split.hasNext(), "bad translation rule: " + line);
                String search = split.next();

                Preconditions.checkState(split.hasNext(), "bad translation rule: " + line);
                String replace = split.next();

                builder.put(search, replace);

            } catch (RuntimeException e) {
                throw new RuntimeException("Problem building translation rule from line: " + line, e);
            }
        }
        return new TranslationTable(builder.build());
    }

    public static TranslationTable makeFromCsvStrings(String... replacements) {
        return makeFromCsvLines(Arrays.asList(replacements));
    }

    private final ImmutableMap<String, String> translationMap;
    private final ImmutableSet<String> distinctTargetValues;
    private final ImmutableSet<String> distinctKeysAndValues;
    private final int minSourceWords;
    private final int maxSourceWords;
    private final int minTargetWords;
    private final int maxTargetWords;

    public TranslationTable(ImmutableMap<String, String> translationMap) {

        this.translationMap = translationMap;
        this.distinctTargetValues = ImmutableSet.copyOf(translationMap.values());
        this.distinctKeysAndValues = ImmutableSet.<String>builder()
                .addAll(distinctTargetValues)
                .addAll(translationMap.keySet())
                .build();

        Pair<Integer, Integer> sourceMinMax = getMinMaxWordCount(translationMap.keySet());
        Pair<Integer, Integer> targetMinMax = getMinMaxWordCount(distinctTargetValues());
        this.minSourceWords = sourceMinMax.getLeft();
        this.maxSourceWords = sourceMinMax.getRight();
        this.minTargetWords = targetMinMax.getLeft();
        this.maxTargetWords = targetMinMax.getRight();
    }

    private static Pair<Integer, Integer> getMinMaxWordCount(Collection<String> values) {
        if (values.size() == 0)
            return Pair.of(0, 0);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (String value : values) {
            int wordCount = StringUtils.countMatches(value, " ") + 1;
            min = Math.min(wordCount, min);
            max = Math.max(wordCount, max);
        }
        return Pair.of(min, max);
    }

    public Iterable<Entry<String,String>> getAllTranslations() {
        return translationMap.entrySet();
    }

    public ImmutableMap<String,String> getTranslationMap() {
        return translationMap;
    }

    @Nonnull
    public String translateOrSame(String source) {
        String maybeResult = translationMap.get(source);
        if (maybeResult != null)
            return maybeResult;

        return source;
    }

    public boolean contains(String value) {
        return distinctKeysAndValues.contains(value);
    }

    @Nullable
    public String translateOrNull(String source) {
        String maybe = translationMap.get(source);
        if (maybe != null)
            return maybe;

        if (distinctTargetValues.contains(source)) {
            return source;
        }
        return null;
    }

    public ImmutableSet<String> distinctTargetValues() {
        return distinctTargetValues;
    }

    public ImmutableSet<String> getDistinctKeysAndValues() {
        return distinctKeysAndValues;
    }

    public int getMinSourceWords() {
        return minSourceWords;
    }

    public int getMaxSourceWords() {
        return maxSourceWords;
    }

    public int getMinTargetWords() {
        return minTargetWords;
    }

    public int getMaxTargetWords() {
        return maxTargetWords;
    }

    public int getMinWords() {
        return Math.min(getMinSourceWords(), getMinTargetWords());
    }

    public int getMaxWords() {
        return Math.max(getMaxSourceWords(), getMaxTargetWords());
    }
}
