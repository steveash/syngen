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

import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;

/**
 * Semantic labels for name tokens
 * @author Steve Ash
 */
public enum NamePart {

    Prefix,
    GivenTag,
    GivenObj(true),
    GivenName(true),
    FirstInitial,
    MiddleName(true),
    MiddleInitial,
    SurnameTag, // when you have constructed, transliterated names (AL SAUD) then use tag/obj
    SurnameObj(true),
    Surname(true),
    LastInitial,
    SuffixGenerational,
    SuffixOther,
    Nickname(true),   // only for obvious aliases like when theyre in quotes or "AKA buddy"
    Particle,   // particle in a name like the "de" and "la" in "de la cruz" (I dont think i use this anywhere instead preferring structure like tag+obj)

    LineageTag,    // the tag starting a patronym or dynasty (like "al" in "al whatever")
    LineageObj(true),    // the result of a patroynm; there should always be Tag->obj
    Lineage(true),       // if its all one token like bin-whatever or ibd-whatever (no tag in front)

    And,            // the conjunction that specifically indicates two _different_ people's names shoved in one name field
    AKA,            // the prefix of an alias (probably comes before a nickname
    NonNamePhrase,  // "on the hill", "up the river"
    Skip,           // some kind of delimiter that should just be treated as nothing
    Duplicate,      // when a name token is repeated
    //
    BabyAlias,      // "baby x boy", "baby boy x"
    LongHonor,      // an honorific like "attorny at law" or "the magnificant"
    Unknown
    ;

    private final boolean maybeIdentifyingInfo;

    NamePart(boolean maybeIdentifyingInfo) {
        this.maybeIdentifyingInfo = maybeIdentifyingInfo;
    }
    NamePart() {
        this(false);
    }

    public boolean isMaybeIdentifyingInfo() {
        return maybeIdentifyingInfo;
    }

    private static final ImmutableBiMap<String,NamePart> codeToPart = ImmutableBiMap.<String,NamePart>builder()
            .putAll(Stream.of(NamePart.values())
                    .map(n -> Pair.of(n.name().toLowerCase(), n))
                    .collect(Guavate.entriesToMap())
            ).build();

    public static NamePart byName(String name) {
        NamePart maybe = codeToPart.get(name.toLowerCase());
        return Preconditions.checkNotNull(maybe, "no label for ", name);
    }

    public static boolean containsName(String name) {
        return codeToPart.containsKey(name.toLowerCase());
    }

    public static boolean isGivenOrMiddle(NamePart part) {
        switch (part) {
            case FirstInitial:
            case GivenName:
            case GivenTag:
            case GivenObj:
            case MiddleInitial:
            case MiddleName:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSurname(NamePart part) {
        switch (part) {
            case Surname:
            case SurnameTag:
            case SurnameObj:
                return true;
            default:
                return false;
        }
    }
    public static boolean isSurnameOrLineage(NamePart part) {
        if (isSurname(part)) {
            return true;
        }
        switch (part) {
            case Lineage:
            case LineageObj:
            case LineageTag:
                return true;
            default:
                return false;
        }
    }

    public static boolean isGivenIdentifying(NamePart part) {
        switch (part) {
            case MiddleName:
            case GivenName:
            case GivenObj:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSurnameIdentifying(NamePart part) {
        switch (part) {
            case Surname:
            case SurnameObj:
            case Lineage:
            case LineageObj:
                return true;
            default:
                return false;
        }
    }
}
