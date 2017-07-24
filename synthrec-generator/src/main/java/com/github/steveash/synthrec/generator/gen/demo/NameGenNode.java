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

package com.github.steveash.synthrec.generator.gen.demo;

import static com.github.steveash.synthrec.Constants.FAMILY_NAME;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.FAMILY_NAME_STRUCT;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME_CULTURE;
import static com.github.steveash.synthrec.Constants.GIVEN_NAME_STRUCT;
import static com.github.steveash.synthrec.Constants.MIDDLE_NAME;
import static com.github.steveash.synthrec.Constants.PREFIX_NAME;
import static com.github.steveash.synthrec.Constants.SEX;
import static com.github.steveash.synthrec.Constants.SUFFIX_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.name.NameEntryField;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.stat.ISampler;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.stat.SequenceConditionalSampler;
import com.github.steveash.synthrec.string.PatternExpander;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;

/**
 * This samples the given and family name struct, then builds up the final name and emits it
 * @author Steve Ash
 */
public class NameGenNode implements GenNode {

    public static final ImmutableBiMap<String, NameEntryField> NAME_CONST_TO_ENUM = ImmutableBiMap.of(
            PREFIX_NAME, NameEntryField.Prefix,
            GIVEN_NAME, NameEntryField.FirstName,
            MIDDLE_NAME, NameEntryField.MiddleName,
            FAMILY_NAME, NameEntryField.LastName,
            SUFFIX_NAME, NameEntryField.Suffix
    );

    public static final ImmutableSet<String> COND_ON = ImmutableSet.of(
            GIVEN_NAME_CULTURE,
            FAMILY_NAME_CULTURE,
            SEX
    );

    private final Map<NamePart, ? extends ISampler<String>> nameSamplers;
    private final PatternExpander expander;
    private final Sampler<String> randomWord;

    public NameGenNode(Map<NamePart, ? extends ISampler<String>> nameSamplers,
            PatternExpander expander, Sampler<String> randomWord
    ) {
        this.nameSamplers = nameSamplers;
        this.expander = expander;
        this.randomWord = randomWord;
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        AssignmentInstance assign = assignment.subset(COND_ON);
        NameSampleBuilder builder = new NameSampleBuilder(rand, assign, expander);
        sampleForStruct(builder, (FieldSketch) assignment.get(GIVEN_NAME_STRUCT));
        sampleForStruct(builder, (FieldSketch) assignment.get(FAMILY_NAME_STRUCT));

        for (Entry<String, NameEntryField> entry : NAME_CONST_TO_ENUM.entrySet()) {
            String outputField = entry.getKey();
            NameEntryField enumField = entry.getValue();
            String value = builder.getFinalValue(enumField);
            assignment.put(outputField, value);
        }
        return true;
    }

    private void sampleForStruct(NameSampleBuilder builder, FieldSketch sketch) {
        builder.initStruct(sketch);
        // first pass fills in everything that doesn't have dependencies on other things
        int i = 0;
        int dupCount = 0;
        int firstDupIndex = -1;
        while (i < sketch.size()) {
            if (sketch.isLiteralValue(i)) {
                builder.setStructSample(i, sketch.getComponentAs(i, String.class));
                i += 1; // move to next
                continue;
            }
            NamePart part = builder.getPart(i);
            // need to sample, there are a few special cases:
            if (part == NamePart.Duplicate) {
                dupCount += 1;
                if (firstDupIndex < 0) {
                    firstDupIndex = i;
                }
                i += 1;
                continue;
            }

            ISampler<String> sampler = checkNotNull(nameSamplers.get(part), "no sampler for part", part);
            if (sampler instanceof SequenceConditionalSampler) {
                i += emitSequence(builder, (SequenceConditionalSampler<String>) sampler, i);
                continue;
            }
            builder.setStructSampleAsResult(i, sampler);
            i += 1;
        }

        // second pass to fix anything that we skipped on the first pass
        if (dupCount > 0) {
            emitDups(builder, dupCount, firstDupIndex, randomWord);
        }

        builder.flushStruct();
    }

    @VisibleForTesting
    static void emitDups(NameSampleBuilder builder, int dupCount, int dupi, Sampler<String> randomWord) {
        // look for common pattern of 1-2 dup tokens, resort to grabbing whatever you can
        if (dupCount == 1) {
            if (tryOneDupSurrounding(builder, dupi)) return;
        }
        // two consecutive duplicates
        if (dupCount == 2 && builder.getPart(dupi + 1) == NamePart.Duplicate) {
            // try to find two identifying proceeding or succeeding following, if that doesn't work then
            // just two before or after
            if (setTwoDupFromIndexes(builder, dupi - 2, dupi - 1, dupi, dupi + 1)) return;
            if (setTwoDupFromIndexes(builder, dupi - 3, dupi - 1, dupi, dupi + 1)) return;
            if (setTwoDupFromIndexes(builder, dupi + 2, dupi + 3, dupi, dupi + 1)) return;
            if (setTwoDupFromIndexes(builder, dupi + 2, dupi + 4, dupi, dupi + 1)) return;
        }

        // try to fit surrounding
        for (int i = 0; i < builder.getStructSize() && dupCount > 0; i++) {
            NamePart part = builder.getPart(i);
            if (part != NamePart.Duplicate) continue;
            if (!tryOneDupSurrounding(builder, i)) {
                builder.setStructSampleAsResult(i, randomWord);
            }
            dupCount -= 1;
        }
        Preconditions.checkState(dupCount == 0);
    }

    private static boolean tryOneDupSurrounding(NameSampleBuilder builder, int dupi) {
        if (setOneDupFromIndex(builder, dupi - 1, dupi, true)) return true;
        if (setOneDupFromIndex(builder, dupi + 1, dupi, true)) return true;
        if (setOneDupFromIndex(builder, dupi - 2, dupi, true)) return true;
        if (setOneDupFromIndex(builder, dupi + 2, dupi, true)) return true;
        if (setOneDupFromIndex(builder, dupi - 1, dupi, false)) return true;
        if (setOneDupFromIndex(builder, dupi + 1, dupi, false)) return true;
        return false;
    }

    private static boolean setTwoDupFromIndexes(NameSampleBuilder builder,
            int src1,
            int src2,
            int dupTarget1,
            int dupTarget2
    ) {
        if (builder.isStructValuePresent(src1) &&
                builder.isStructValuePresent(src2) &&
                isIdentifying(builder.getPart(src1)) &&
                isIdentifying(builder.getPart(src2))
                ) {
            builder.setStructSample(dupTarget1, builder.getStructValue(src1));
            builder.setStructSample(dupTarget2, builder.getStructValue(src2));
            return true;
        }
        return false;
    }

    private static boolean isIdentifying(NamePart part) {
        return NamePart.isGivenIdentifying(part) || NamePart.isSurnameIdentifying(part);
    }

    private static boolean setOneDupFromIndex(NameSampleBuilder builder,
            int srci,
            int dupi,
            boolean requireIdentifying
    ) {
        if (builder.isStructValuePresent(srci)) {
            if (!requireIdentifying || isIdentifying(builder.getPart(srci))) {
                builder.setStructSample(dupi, builder.getStructValue(srci));
                return true;
            }
        }
        return false;
    }

    private int emitSequence(NameSampleBuilder builder, SequenceConditionalSampler<String> sampler, final int index) {
        int maxToSample = builder.consecutivePartsAt(index);
        List<String> sampled = sampler.sample(builder.getRand(), maxToSample, builder.getConditionedOn());
        Preconditions.checkState(sampled.size() > 0 && sampled.size() <= maxToSample);
        for (int i = 0; i < sampled.size(); i++) {
            builder.setStructSample(index + i, sampled.get(i));
        }
        return sampled.size();
    }

    private boolean shouldSkipNow(NamePart part) {
        return part == NamePart.Duplicate;
    }

    private NameEntryField outputKeyFor(String outputField) {
        switch (outputField) {

            default:
                throw new IllegalStateException("dont know field " + outputKeys());
        }
    }

    @Override
    public Set<String> inputKeys() {
        return ImmutableSet.of(
                GIVEN_NAME_STRUCT,
                FAMILY_NAME_STRUCT,
                GIVEN_NAME_CULTURE,
                FAMILY_NAME_CULTURE,
                SEX
        );
    }

    @Override
    public Set<String> outputKeys() {
        return NAME_CONST_TO_ENUM.keySet();
    }

    @Override
    public String toString() {
        return "NameGenNode{}";
    }
}
