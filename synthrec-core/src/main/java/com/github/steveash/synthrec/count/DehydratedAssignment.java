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

import java.io.Serializable;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2IntArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap.Entry;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;

/**
 * An assignment is a set of pairs where each pair is distribution_name -> value from distribution
 * The VocabHydrator knows how to take a real assignment and turn it into a dehydrated assignment of just ids
 *
 * Note that flat distribs are just recorded in the flatAssigns map as distribCode -> vocabIndex and
 * hierarch distribs are recorded as distribCode -> subFieldCode -> vocabIndex maps where if the vocabIndex is 0
 * (which isnt a legal value) that means that the placeholder value in the sketch is all there is; if its != 0 then
 * its a literal value
 *
 * @author Steve Ash
 */
public class DehydratedAssignment implements Serializable {

    private static final long serialVersionUID = 529879683700478587L;

    /**
     * Creates a new map instance for the countAssignmentInstance size -- this encapsulates the policy of
     * what size results in what underlying map implementation (only different ones so still no megamorphic
     * call site deopt
     * @param size
     * @return
     */
    static Short2IntMap createMap(int size) {
        if (size <= 4) {
            return new Short2IntArrayMap(size);
        }
        return new Short2IntOpenHashMap(size);
    }

    public static DehydratedAssignment merge(DehydratedAssignment a, DehydratedAssignment b) {
        Short2IntMap flat = createMap(a.flatAssigns.size() + b.flatAssigns.size());
        putAll(a.flatAssigns, flat);
        putAll(b.flatAssigns, flat);
        Short2ObjectMap<Short2IntArrayMap> hierarch = null;
        int hcount = 0;
        if (a.hierarchAssigns != null) {
            hcount += a.hierarchAssigns.size();
        }
        if (b.hierarchAssigns != null) {
            hcount += b.hierarchAssigns.size();
        }
        if (hcount > 0) {
            hierarch = new Short2ObjectArrayMap<>(hcount);
        }
        if (a.hierarchAssigns != null) {
            putAllh(a.hierarchAssigns, hierarch);
        }
        if (b.hierarchAssigns != null) {
            putAllh(b.hierarchAssigns, hierarch);
        }
        return new DehydratedAssignment(flat, hierarch);
    }

    private static void putAllh(Short2ObjectMap<Short2IntArrayMap> source,
            Short2ObjectMap<Short2IntArrayMap> sink
    ) {
        ObjectIterator<Short2ObjectMap.Entry<Short2IntArrayMap>> iter = source.short2ObjectEntrySet().iterator();
        while (iter.hasNext()) {
            Short2ObjectMap.Entry<Short2IntArrayMap> entry = iter.next();
            Short2IntArrayMap prev = sink.put(entry.getShortKey(), entry.getValue());
            Preconditions.checkState(prev == null, "cant merge two with entries for the same subfield");
        }
    }

    private static void putAll(Short2IntMap source, Short2IntMap sink) {
        ObjectIterator<Entry> iter1 = source.short2IntEntrySet().iterator();
        while (iter1.hasNext()) {
            Entry next = iter1.next();
            sink.put(next.getShortKey(), next.getIntValue());
        }
    }

    private final Short2IntMap flatAssigns;
    @Nullable private final Short2ObjectMap<Short2IntArrayMap> hierarchAssigns;
    private final int cachedHash;

    public DehydratedAssignment(Short2IntMap flatAssigns, @Nullable Short2ObjectMap<Short2IntArrayMap> hierarchAssigns) {
        this.flatAssigns = flatAssigns;
        this.hierarchAssigns = hierarchAssigns;
        int hash = 17 * 37 + flatAssigns.hashCode();
        if (hierarchAssigns != null) {
            hash = hash * 37 + hierarchAssigns.hashCode();
        }
        this.cachedHash = hash;
    }

    public boolean isEmpty() {
        if (hierarchAssigns != null && !hierarchAssigns.isEmpty()) {
            return false;
        }
        return flatAssigns.isEmpty();
    }

    public int size() {
        return flatAssigns.size() + (hierarchAssigns != null ? hierarchAssigns.size() : 0);
    }

    public Short2IntMap getFlatAssigns() {
        return flatAssigns;
    }

    @Nullable
    public Short2ObjectMap<Short2IntArrayMap> getHierarchAssigns() {
        return hierarchAssigns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DehydratedAssignment that = (DehydratedAssignment) o;

        if (cachedHash != that.cachedHash) return false;
        if (!flatAssigns.equals(that.flatAssigns)) return false;
        return hierarchAssigns != null ? hierarchAssigns.equals(that.hierarchAssigns) : that.hierarchAssigns == null;
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    @Override
    public String toString() {
        return "DehydratedAssignment{" +
                "flatAssigns=" + flatAssigns +
                ", hierarchAssigns=" + hierarchAssigns +
                '}';
    }
}
