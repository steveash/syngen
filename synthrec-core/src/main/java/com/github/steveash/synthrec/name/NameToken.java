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

package com.github.steveash.synthrec.name;

import java.util.List;
import java.util.Set;

import com.github.steveash.synthrec.canonical.NormalToken;
import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;

/**
 * One token of a name includes both original and various processed verisons of the token; plus has a spot
 * for labels
 * @author Steve Ash
 */
public class NameToken implements NormalToken {

    public static NameToken make(String original,
            String normalWithPunc,
            String normalNoPunc,
            String dictNormal,
            Set<String> phoneEncodings,
            NameEntryField entryField
    ) {
        return new NameToken(
                original, normalWithPunc, normalNoPunc, dictNormal, phoneEncodings, NamePart.Unknown,
                ImmutableList.of(), entryField
        );
    }

    private final String original;  // original token from tokenizer
    private final String normalWithPunc;    // the normalized version that has all but semantic punctuation removed
    private final String normalNoPunc;  // the completely stripped version (only letters)
    private final String dictNormal; // the normalized version for dict lookup (might be slightly diff from above normal)
    private final Set<String> phoneEncodings; // the phonetic encoded version
    private final NamePart part;
    private final NameEntryField field; // if the field where this token came from is known then put it here
    private final List<Object2DoubleMap.Entry<String>> cultures;

    private NameToken(String original,
            String normalWithPunc,
            String normalNoPunc,
            String dictNormal,
            Set<String> phoneEncodings,
            NamePart part,
            List<Entry<String>> cultures,
            NameEntryField entryField
    ) {
        this.original = original;
        this.normalWithPunc = normalWithPunc;
        this.normalNoPunc = normalNoPunc;
        this.dictNormal = dictNormal;
        this.phoneEncodings = phoneEncodings;
        this.part = part;
        this.cultures = cultures; // maybe null if not set
        this.field = entryField;
    }

    public String getOriginal() {
        return original;
    }

    public String getNormalWithPunc() {
        return normalWithPunc;
    }

    public String getNormalNoPunc() {
        return normalNoPunc;
    }

    public String getDictNormal() {
        return dictNormal;
    }

    public Set<String> getPhoneticEncoded() {
        return phoneEncodings;
    }

    public NamePart getPart() {
        return part;
    }

    public List<Entry<String>> getCultures() {
        return cultures;
    }

    public NameEntryField getEntryField() {
        return field;
    }

    public NameToken withPart(NamePart newPart) {
        return new NameToken(original, normalWithPunc, normalNoPunc, dictNormal, phoneEncodings, newPart,
                cultures, field);
    }

    public NameToken withCultures(List<Object2DoubleMap.Entry<String>> newCultures) {
        return new NameToken(original, normalWithPunc, normalNoPunc, dictNormal, phoneEncodings, part, newCultures,
                field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NameToken nameToken = (NameToken) o;

        return original.equals(nameToken.original);
    }

    @Override
    public int hashCode() {
        return original.hashCode();
    }

    @Override
    public String toString() {
        return "NameToken{" +
                "original='" + original + '\'' +
                ", normalWithPunc='" + normalWithPunc + '\'' +
                ", normalNoPunc='" + normalNoPunc + '\'' +
                ", part=" + part +
                '}';
    }

    // so that you can pass this into the other components without adapting
    @Override
    public String getOriginalToken() {
        return original;
    }

    @Override
    public String getNormalToken() {
        return dictNormal;
    }
}
