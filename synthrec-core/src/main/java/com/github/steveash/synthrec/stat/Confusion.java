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

package com.github.steveash.synthrec.stat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.function.ToIntFunction;

import com.github.steveash.jg2p.util.Percent;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.inamik.text.tables.Cell.Function;
import com.inamik.text.tables.Cell.Functions;
import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.SimpleTable;
import com.inamik.text.tables.grid.Border;
import com.inamik.text.tables.grid.Util;
import com.inamik.text.tables.line.LeftPad;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Confusion matrix implementation that does simple counts and pretty printing
 * @author Steve Ash
 */
public class Confusion<T> {

    public static <T> Confusion<T> makeForOutcomes(T... outcomes) {
        Object2IntOpenHashMap<T> map = new Object2IntOpenHashMap<>(outcomes.length);
        map.defaultReturnValue(-1);
        for (int i = 0; i < outcomes.length; i++) {
            map.put(outcomes[i], i);
        }
        return new Confusion<T>(t -> {
            int idx = map.getInt(t);
            if (idx < 0) {
                throw new IllegalArgumentException("No outcome for " + t);
            }
            return idx;
        }, ImmutableList.copyOf(outcomes));
    }

    private static final int BOARD_WIDTH = 75;
    private final ToIntFunction<T> toIndex;
    private final IntArrayList counts;
    private final ImmutableList<T> targets;

    private Confusion(ToIntFunction<T> toIndex, List<T> targets) {
        this.toIndex = toIndex;
        this.targets = ImmutableList.copyOf(targets);
        this.counts = new IntArrayList(targets.size());
        for (int i = 0; i < targets.size() * targets.size(); i++) {
            this.counts.add(0);
        }
    }

    public void reset() {
        for (int i = 0; i < counts.size(); i++) {
            counts.set(i, 0);
        }
    }

    public Confusion<T> increment(T actual, T predicted) {
        return add(actual, predicted, 1);
    }

    public Confusion<T> add(T actual, T predicted, int delta) {
        int countIdx = countIndex(actual, predicted);
        counts.set(countIdx, counts.getInt(countIdx) + delta);
        return this;
    }

    private int countIndex(T actual, T predicted) {
        return (toIndex.applyAsInt(actual) * targets.size()) + toIndex.applyAsInt(predicted);
    }

    public String toTableString() {
        SimpleTable s = SimpleTable.of();
        s.nextRow()
            .nextCell("Act \\ Pred");
        targets.forEach(t -> s.nextCell(t.toString()));
        s.nextCell("***SUM***");

        for (T act : targets) {
            s.nextRow();
            s.nextCell(act.toString());
            int sum = 0;
            for (T pred : targets) {
                int thisCount = counts.getInt(countIndex(act, pred));
                sum += thisCount;
                s.nextCell(Integer.toString(thisCount));
            }
            s.nextCell(Integer.toString(sum));
        }
        s.nextRow();
        s.nextCell("***SUM***");
        int total = 0;
        int totalRight = 0;
        for (T pred : targets) {
            int sum = 0;
            for (T act : targets) {
                int thisCount = counts.getInt(countIndex(act, pred));
                sum += thisCount;
                if (pred.equals(act)) {
                    totalRight += thisCount;
                }
            }
            s.nextCell(Integer.toString(sum));
            total += sum;
        }
        s.nextCell(Integer.toString(total), Percent.print(totalRight, total));

        GridTable grid = s.toGrid();
        grid = Border.SINGLE_LINE.apply(grid);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos, false, Charsets.UTF_8.name());
            Util.print(grid, ps);
            ps.close();
            return baos.toString(Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
