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

package com.github.steveash.synthrec.count;

import java.util.List;
import java.util.Map.Entry;

import com.github.steveash.synthrec.collect.Vocabulary;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2IntArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * Hydrator that knows how to dehydrate a CountAssignmentInstance given a DistribRegistry
 *
 * Also note that during the counting process -- it's dehydration that actually populated the underlying vocabularies
 * @author Steve Ash
 */
public class VocabHydrater {

    private final DistribVocabRegistry registry;

    public VocabHydrater(DistribVocabRegistry registry) {
        this.registry = registry;
    }

    public DehydratedAssignment dehydrate(AssignmentInstance instance) {
        Short2IntMap flatVals = DehydratedAssignment.createMap(instance.size());
        Short2ObjectMap<Short2IntArrayMap> hierarchVals = null;
        for (Entry<String, Object> entry : instance.getAssignment().entrySet()) {

            short code = registry.resolveDistribCode(entry.getKey());
            Object value = entry.getValue();

            if (value instanceof Multinomial) {
                throw new IllegalStateException("Didnt expect to get multinomials in here");

            } else if (value instanceof FieldSketch) {
                if (hierarchVals == null) {
                    hierarchVals = new Short2ObjectArrayMap<>(1);
                }
                Short2IntArrayMap sketch = dehydrateHierarch(entry.getKey(), (FieldSketch) entry.getValue());
                hierarchVals.put(code, sketch);

            } else {

                int index = registry.resolveValueIndexFor(entry.getKey(), entry.getValue());
                flatVals.put(code, index);
            }
        }
        return new DehydratedAssignment(flatVals, hierarchVals);
    }

    private Short2IntArrayMap dehydrateHierarch(String distribName, FieldSketch sketch) {
        Short2IntArrayMap map = new Short2IntArrayMap(sketch.size());
        for (int i = 0; i < sketch.size(); i++) {
            short subFieldCode = registry.resolveDistribSubFieldCode(distribName, sketch.getSketchField(i));
            Vocabulary<Object> vocab = registry.resolveVocab(subFieldCode);
            // always put it so that we record the full literal distribution
            int valueIndex = vocab.putIfAbsent(sketch.getLiteralField(i));
            int valueToUse = 0;
            if (sketch.isLiteralValue(i)) {
                valueToUse = valueIndex;
            }
            map.put(subFieldCode, valueToUse);
        }
        return map;
    }

    public MutableMultinomial<List<Object>> hydrateMultinomialToList(Multinomial<DehydratedAssignment> input, List<String> outputList) {
        MutableMultinomial<List<Object>> output = new MutableMultinomial<>(-1);
        ObjectIterator<Object2DoubleMap.Entry<DehydratedAssignment>> iter = input.entries().fastIterator();
        while (iter.hasNext()) {
            Object2DoubleMap.Entry<DehydratedAssignment> entry = iter.next();
            AssignmentInstance instance = hydrate(entry.getKey());
            List<Object> val = Lists.newArrayListWithCapacity(outputList.size());
            for (String key : outputList) {
                val.add(instance.get(key, null));
            }
            output.add(val, entry.getDoubleValue());
        }
        return output;
    }

    public MutableMultinomial<Object> hydrateMultinomialToUnary(Multinomial<DehydratedAssignment> input, String outputKey) {
        MutableMultinomial<Object> output = new MutableMultinomial<>(-1);
        ObjectIterator<Object2DoubleMap.Entry<DehydratedAssignment>> iter = input.entries().fastIterator();
        while (iter.hasNext()) {
            Object2DoubleMap.Entry<DehydratedAssignment> entry = iter.next();
            AssignmentInstance instance = hydrate(entry.getKey());
            output.add(instance.get(outputKey, null), entry.getDoubleValue());
        }
        return output;
    }

    public AssignmentInstance hydrate(DehydratedAssignment assignment) {
        Builder<String, Object> builder = ImmutableMap.builder();
        for (Short2IntMap.Entry entry : assignment.getFlatAssigns().short2IntEntrySet()) {
            short code = entry.getShortKey();
            String name = registry.resolveNameForCode(code);
            Object value = registry.resolveValueForIndex(code, entry.getIntValue());
            builder.put(name, value);
        }
        if (assignment.getHierarchAssigns() != null) {
            for (Short2ObjectMap.Entry<Short2IntArrayMap> entry : assignment.getHierarchAssigns().short2ObjectEntrySet()) {
                short distribCode = entry.getShortKey();
                String distribName = registry.resolveNameForCode(distribCode);
                Short2IntArrayMap vals = entry.getValue();

                FieldSketch newSketch = hydrateHierarch(vals);
                builder.put(distribName, newSketch);
            }
        }
        return AssignmentInstance.make(builder.build());
    }

    private FieldSketch hydrateHierarch(Short2IntArrayMap vals) {
        FieldSketch.Builder sketchBuilder = FieldSketch.builder();

        // arraymap is ordered by insert order so its linked in that way
        ObjectIterator<Short2IntMap.Entry> iter = vals.short2IntEntrySet().fastIterator();
        while (iter.hasNext()) {
            Short2IntMap.Entry sketchEntry = iter.next();
            short subFieldCode = sketchEntry.getShortKey();
            String subFieldName = registry.resolveNameForCode(subFieldCode);

            int valueIndex = sketchEntry.getIntValue();
            if (valueIndex != 0) {
                Object subFieldValue = registry.resolveSubFieldValueForIndex(subFieldCode, valueIndex);
                sketchBuilder.addLiteral(subFieldName, subFieldValue);
            } else {
                sketchBuilder.addPlaceholder(subFieldName, null);
            }
        }

        return sketchBuilder.build();
    }
}
