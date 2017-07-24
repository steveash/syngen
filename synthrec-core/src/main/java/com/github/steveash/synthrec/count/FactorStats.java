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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.github.steveash.synthrec.collect.VocabCounter;
import com.github.steveash.synthrec.domain.FieldSketch;
import com.github.steveash.synthrec.domain.MissingPolicy;
import com.github.steveash.synthrec.domain.Multivalue;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

/**
 * General descriptive statistics for each factor that a CountDag counts
 * @author Steve Ash
 */
public class FactorStats implements Serializable {

    private static final long serialVersionUID = -1763968297907993826L;

    private final String factorName;
    private final DistribVocabRegistry vocabRegistry;

    private int presentCount = 0;
    private int missingCount = 0;
    private int groundedCount = 0;  // how many times did we see grounded values
    private int sketchCount = 0;   // how many times did we see sketches
    private int distribCount = 0;   // how many times did we see distributions instead of grounded values

    private boolean sawNumber = false;
    //    private boolean sawString = false;
    private final SummaryStatistics numberStats = new SummaryStatistics();

    // for this factor's vocab (when flat) we record the counts by vocabIndex
    private final VocabCounter<Object> valueDist;
    private final Set<String> subFieldNames = Sets.newHashSet();
    private final Short2ObjectOpenHashMap<VocabCounter<Object>> subFieldCodeToCounter = new Short2ObjectOpenHashMap<>();

    public FactorStats(String factorName, DistribVocabRegistry vocabRegistry) {
        this.factorName = factorName;
        this.vocabRegistry = vocabRegistry;
        this.valueDist = new VocabCounter<>(vocabRegistry.resolveVocabForDistrib(factorName));
    }

    public boolean isSketches() {
        if (sketchCount > 0) {
            Preconditions.checkState(groundedCount == 0,
                    "got sketches and grounded values",
                    sketchCount,
                    groundedCount
            );
            Preconditions.checkState(!subFieldNames.isEmpty());
            return true;
        }
        Preconditions.checkState(subFieldNames.isEmpty());
        return false;
    }

    public VocabCounter<Object> getValueVocab() {
        return valueDist;
    }

    public VocabCounter<Object> getSubFieldVocab(String subFieldName) {
        return resolveCounter(subFieldName);
    }

    public Set<String> getSubFieldNames() {
        return subFieldNames;
    }

    /**
     * Returns a map of subfield -> multinomial for this particular node's structured fields; throws an
     * exception if this isn't a "sketch" field (i.e. it isn't recording hierarchical stuff)
     * @return
     */
    public Map<String,MutableMultinomial<Object>> makeSubfieldUnaryCopy() {
        Preconditions.checkState(isSketches(), "cant get a hydrated copy of subfields if its not a sketch field");
        HashMap<String, MutableMultinomial<Object>> result = Maps.newHashMapWithExpectedSize(subFieldNames.size());
        for (String subFieldName : subFieldNames) {
            VocabCounter<Object> vocab = getSubFieldVocab(subFieldName);
            MutableMultinomial<Object> multi = vocab.convertToMultinomial();
            result.put(subFieldName, multi);
        }
        return result;
    }

    public void onAssignment(Object value) {
        if (value == null || MissingPolicy.PLACEHOLDER.equals(value) || "".equals(value)) {
            missingCount += 1;
            return;
        }
        if (value instanceof Multivalue) {
            for (Object insideValue : ((Multivalue) value).getValueBag()) {
                onAssignment(insideValue);
            }
            return;
        }
        if (value instanceof Multinomial) {
            distribCount += 1;
            Multinomial dens = (Multinomial) value;
            if (dens.isNotEmpty()) {
                Preconditions.checkArgument(dens.isNormalized(), "cant submit a non normalized distribution");
                value = dens.best(); // if its a distribution with entries then lets profile the best value
                addAll(valueDist, dens);
            }
        } else if (value instanceof FieldSketch) {
            presentCount += 1;
            sketchCount += 1;
            FieldSketch sketch = (FieldSketch) value;
            for (int i = 0; i < sketch.size(); i++) {
                String subField = sketch.getSketchField(i);
                this.subFieldNames.add(subField);
                VocabCounter<Object> vocab = resolveCounter(subField);
                vocab.incrementByValue(sketch.getLiteralField(i));
            }
        } else {
            presentCount += 1;
            groundedCount += 1;
            valueDist.incrementByValue(value);
        }

        if (value instanceof Number) {
            numberStats.addValue(((Number) value).doubleValue());
            sawNumber = true;
        }
    }

    private VocabCounter<Object> resolveCounter(String subField) {
        short subFieldCode = vocabRegistry.resolveDistribSubFieldCode(factorName, subField);
        VocabCounter<Object> vocab = subFieldCodeToCounter.get(subFieldCode);
        if (vocab == null) {
            vocab = new VocabCounter<>(vocabRegistry.resolveVocab(subFieldCode));
            subFieldCodeToCounter.put(subFieldCode, vocab);
        }
        return vocab;
    }

    private void addAll(VocabCounter<Object> valueDist, Multinomial<?> multi) {
        ObjectIterator<? extends Object2DoubleMap.Entry<?>> iter = multi.entries().fastIterator();
        while (iter.hasNext()) {
            Object2DoubleMap.Entry<?> entry = iter.next();
            valueDist.addByValue(entry.getKey(), entry.getDoubleValue());
        }
    }

    public void printTo(PrintWriter pw) {
        pw.println(String.format(" **** Factor Stats: %s  present %d, missing %d, uncertain %d; " +
                        "of present %d sketches %d grounded **** ",
                factorName,
                presentCount,
                missingCount,
                distribCount,
                sketchCount,
                groundedCount
        ));
        if (sawNumber) {
            pw.println("  > Number stats:");
            pw.println(numberStats.toString());
        }
//        if (sawString) {
//            pw.println("  > String stats:");
//            HistogramPrint.printHistoTo(pw, stringLenHisto);
//        }
        if (valueDist.isNotEmpty()) {
            pw.println("  > Value dist");
            valueDist.printTo(pw);
        }
        if (!subFieldCodeToCounter.isEmpty()) {
            pw.println("  > SubField dist");
            for (Entry<Short, VocabCounter<Object>> entry : subFieldCodeToCounter.entrySet()) {
                String name = vocabRegistry.resolveNameForCode(entry.getKey());
                pw.println("  >> " + name);
                entry.getValue().printTo(pw);
            }
        }
    }
}

