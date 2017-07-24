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

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.event.InputMethodListener;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.string.PatternExpander;
import com.github.steveash.synthrec.string.TokenStringBuilder;
import com.google.common.collect.ImmutableSet;

/**
 * A generatic gennode that knows how to sample from a fieldsketch
 * @author Steve Ash
 */
public class FieldSketchGenNode implements GenNode {

    private final String inputKey;
    private final String outputKey;
    private final Function<String, ? extends Sampler<?>> subFieldSampling;
    private final PatternExpander expander;

    public FieldSketchGenNode(
            String inputKey,
            String outputKey,
            Function<String, ? extends Sampler<?>> subFieldSampling,
            PatternExpander expander
    ) {
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.subFieldSampling = subFieldSampling;
        this.expander = expander;
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        FieldSketch sketch = (FieldSketch) assignment.get(inputKey);
        TokenStringBuilder sb = new TokenStringBuilder();
        sampleSketch(rand, sketch, sb);
        String value = sb.toString();
        assignment.put(outputKey, value);
        return true;
    }

    public void sampleSketch(RandomGenerator rand, FieldSketch sketch, TokenStringBuilder sb) {
        for (int i = 0; i < sketch.size(); i++) {
            String subfield;
            if (sketch.isLiteralValue(i)) {
                subfield = sketch.getComponentAs(i, String.class);
            } else {
                subfield = replace(rand, sketch.getSketchField(i));
            }
            sb.append(expander.expandIfNeeded(rand, subfield));
        }
    }

    private String replace(RandomGenerator rand, String sketchField) {
        return (String) checkNotNull(subFieldSampling.apply(sketchField), "no subfield", sketchField)
                .sample(rand);
    }

    @Override
    public Set<String> inputKeys() {
        return ImmutableSet.of(inputKey);
    }

    @Override
    public Set<String> outputKeys() {
        return ImmutableSet.of(outputKey);
    }
}
