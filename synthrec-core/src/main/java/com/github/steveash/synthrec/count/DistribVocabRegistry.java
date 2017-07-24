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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.github.steveash.synthrec.collect.Vocabulary;
import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortMap;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

/**
 *  Manages mappings of distributionName to codes (shorts) up to 64k of these allowed
 *  Each distribution code -> Vocab
 *  For hierarchical distributions it manages the mapping from (distributionName,subField) -> Vocab
 * @author Steve Ash
 */
public class DistribVocabRegistry implements Serializable {

    private static final long serialVersionUID = 369399985251589651L;

    private short nextDistribCode = 1;
    // non-sketch distributions
    private final Object2ShortOpenHashMap<String> distribNameToCode = new Object2ShortOpenHashMap<>();
    // sketch distributions are distribName -> sub field obj -> distrib+subfield code
    private final Object2ObjectOpenHashMap<String, Object2ShortOpenHashMap<String>> distribNameToFieldToCode = new Object2ObjectOpenHashMap<>();

    private final Short2ObjectOpenHashMap<String> codeToName = new Short2ObjectOpenHashMap<>();
    // all codes come monotonically from the same counter so we can just densely pack this array
    private final ArrayList<Vocabulary<Object>> codeToVocab = Lists.newArrayList();
    {
        codeToVocab.add(null); // dont put anything in zero
    }

    public Vocabulary<Object> resolveVocabForDistrib(String distribName) {
        short code = resolveDistribCode(distribName);
        return resolveVocab(code);
    }

    public int resolveValueIndexFor(String distribName, Object value) {
        Vocabulary<Object> vocab = resolveVocabForDistrib(distribName);
        return vocab.putIfAbsent(value);
    }

    public Object resolveValueForIndex(short distribCode, int valueIndex) {
        Vocabulary<Object> vocab = resolveVocab(distribCode);
        return vocab.getForIndex(valueIndex);
    }

    public Vocabulary<Object> resolveSubFieldVocabFor(String distribName, String subField) {
        short code = resolveDistribSubFieldCode(distribName, subField);
        return resolveVocab(code);
    }

    public int resolveSubFieldValueIndexFor(String distribName, String subField, Object value) {
        Vocabulary<Object> vocab = resolveSubFieldVocabFor(distribName, subField);
        return vocab.putIfAbsent(value);
    }

    public Object resolveSubFieldValueForIndex(short subfieldCode, int valueIndex) {
        Vocabulary<Object> vocab = resolveVocab(subfieldCode);
        return vocab.getForIndex(valueIndex);
    }

    public short resolveDistribSubFieldCode(String distribName, String subField) {
        Object2ShortOpenHashMap<String> maybeFieldToCode = distribNameToFieldToCode.get(distribName);
        if (maybeFieldToCode == null) {
            maybeFieldToCode = new Object2ShortOpenHashMap<>();
            distribNameToFieldToCode.put(distribName, maybeFieldToCode);
        }
        short code = maybeFieldToCode.getShort(subField);
        if (code != 0) {
            return code;
        }
        code = allocateNextCode(subField);
        maybeFieldToCode.put(subField, code);
        return code;
    }

    public String resolveNameForCode(short code) {
        return checkNotNull(codeToName.get(code), "no name for code", code);
    }

    public Vocabulary<Object> resolveVocab(short code) {
        return checkNotNull(codeToVocab.get(code), "no vocab for ", code);
    }

    public short resolveDistribCode(String distribName) {
        short code = distribNameToCode.getShort(distribName);
        if (code != 0) {
            return code;
        }
        code = allocateNextCode(distribName);
        distribNameToCode.put(distribName, code);
        return code;
    }

    private short allocateNextCode(String name) {
        short toAdd = nextDistribCode;
        codeToVocab.add(toAdd, new Vocabulary<>());
        codeToName.put(toAdd, name);
        nextDistribCode = Shorts.checkedCast(toAdd + 1);
        return toAdd;
    }

    public String toBigString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, Short> entry : distribNameToCode.entrySet()) {
            String distribName = entry.getKey();
            short distribCode = entry.getValue();
            if (distribNameToFieldToCode.containsKey(distribName)) {
                sb.append("> Hierarch Distrib ").append(distribName).append(" -> ").append(distribCode).append("\n");
                Object2ShortOpenHashMap<String> subFields = distribNameToFieldToCode.get(distribName);
                for (Object2ShortMap.Entry<String> subEntry : subFields.object2ShortEntrySet()) {
                    String subField = subEntry.getKey();
                    short subFieldCode = subEntry.getShortValue();
                    sb.append("+ Sub ").append(subField).append(" -> ").append(subFieldCode).append("\n");
                    appendVocab(sb, subFieldCode);
                }
            } else {
                sb.append("> Flat Distrib ").append(distribName).append(" -> ").append(distribCode).append("\n");
                appendVocab(sb, distribCode);
            }
        }
        return sb.toString();
    }

    private void appendVocab(StringBuilder sb, short value) {Vocabulary<Object> vocab = codeToVocab.get(value);
        for (int i = 1; i < vocab.size(); i++) {
            sb.append(">> ").append(i).append(": ").append(vocab.getForIndex(i)).append("\n");
        }
    }
}
