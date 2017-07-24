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

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.collect.LazyMap;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.generator.feature.NameStructureFeature;
import com.github.steveash.synthrec.name.NameEntryField;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.stat.ConditionalSampler;
import com.github.steveash.synthrec.stat.ISampler;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.string.PatternExpander;
import com.github.steveash.synthrec.string.TokenStringBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * represents a name that is in the process of being sampled one struct at a time
 * usage:
 * building one name? then construct one NameSampleBuilder
 * before each struct sample call:
 * 1- initStruct()
 *      2- then add struct sample values for this struct
 * 3- once all struct values have been set, then flush() to get them emittied into the TokenStringBuilders
 * @author Steve Ash
 */
public class NameSampleBuilder {

    private final RandomGenerator rand;
    private final AssignmentInstance conditionedOn;
    private final PatternExpander expander;
    private final LazyMap<NameEntryField, TokenStringBuilder> fieldBuilders = LazyMap.makeSupply(TokenStringBuilder::new);

    // for each struct these are used
    private List<NamePart> parts = Lists.newArrayList();
    private List<NameEntryField> fields = Lists.newArrayList();
    private List<String> samples = Lists.newArrayList();
    private int structSize;

    public NameSampleBuilder(RandomGenerator rand, AssignmentInstance conditionedOn, PatternExpander expander) {
        this.rand = rand;
        this.conditionedOn = conditionedOn;
        this.expander = expander;
    }

    public void initStruct(FieldSketch sketch) {
        parts.clear();
        fields.clear();
        samples.clear();
        structSize = sketch.size();

        for (int i = 0; i < structSize; i++) {
            String label = sketch.getSketchField(i);
            NamePart part = NameStructureFeature.getNamePartFromLabel(label);
            NameEntryField field = NameStructureFeature.getEntryFieldFromLabel(label);
            parts.add(part);
            fields.add(field);
            samples.add(null); // init to null
        }
    }

    public RandomGenerator getRand() {
        return rand;
    }

    public AssignmentInstance getConditionedOn() {
        return conditionedOn;
    }

    public void setStructSampleAsResult(int index, ISampler<String> sampler) {
        if (sampler instanceof ConditionalSampler) {
            setStructSample(index, ((ConditionalSampler<String>) sampler).sample(rand, conditionedOn));
        } else if (sampler instanceof Sampler) {
            setStructSample(index, ((Sampler<String>) sampler).sample(rand));
        } else {
            throw new IllegalStateException("dont know how to sample from " + sampler);
        }
    }

    public void setStructSample(int index, @Nullable String value) {
        if (value != null) {
            value = expander.expandIfNeeded(rand, value);
        }
        samples.set(index, value);
    }

    public void flushStruct() {
        for (int i = 0; i < structSize; i++) {
            NameEntryField field = fields.get(i);
            String value = Preconditions.checkNotNull(samples.get(i), "null sample in name builder", samples, parts);
            fieldBuilders.get(field).append(value);
        }
        parts.clear();
        fields.clear();
        samples.clear();
        structSize = -1;
    }

    public int consecutivePartsAt(int index) {
        NamePart part = parts.get(index);
        int count = 1;
        for (int i = index + 1; i < parts.size() && parts.get(i) == part; i++) {
            count += 1;
        }
        return count;
    }

    public int getStructSize() {
        return structSize;
    }

    public boolean isStructValuePresent(int index) {
        if (index < 0 || index >= samples.size()) {
            return false;
        }
        return samples.get(index) != null;
    }

    public String getStructValue(int index) {
        return samples.get(index);
    }

    public NamePart getPart(int index) {
        return parts.get(index);
    }

    public NameEntryField getField(int index) {
        return fields.get(index);
    }

    public String getFinalValue(NameEntryField field) {
        // dont want lazy loading for this:
        TokenStringBuilder maybe = fieldBuilders.delegate().get(field);
        if (maybe == null) {
            return "";
        }
        return maybe.toString();
    }
}
