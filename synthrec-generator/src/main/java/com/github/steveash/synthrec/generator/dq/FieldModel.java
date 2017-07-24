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

package com.github.steveash.synthrec.generator.dq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.steveash.kylm.model.immutable.ImmutableLM;
import com.github.steveash.synthrec.string.CharAsStringIterable;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
public class FieldModel implements Serializable {
    private static final long serialVersionUID = 445718635694875638L;

    private enum XformType {
            Character,
            Word
        }

    public FieldModel(XformType xform, ImmutableLM lm) {
        this.xform = xform;
        this.lm = lm;
    }

    private static final Splitter WORD_SPLITTER = Splitter.on(CharMatcher.whitespace())
            .omitEmptyStrings()
            .trimResults();

    private final XformType xform;
    private final ImmutableLM lm;

    public static class ScoreResult {
        public final double perplexity;
        public final double perpNum;
        public final int perDenom;

        public ScoreResult(double perplexity, double perpNum, int perDenom) {
            this.perplexity = perplexity;
            this.perpNum = perpNum;
            this.perDenom = perDenom;
        }
    }

    public ScoreResult score(String input) {
        switch (xform) {
            case Word:
                List<String> sentence = wordsFromInput(input);
                double logProb = -lm.sentenceProb(sentence); // sentenceprob returns log10prob
                int count = sentence.size() + 2; // 2 for the start/end markers
                return new ScoreResult(logProb / count, logProb, count);
            case Character:
                ArrayList<String> sentence1 = charsFromInput(input);
                int count1 = sentence1.size() + 2;
                double logProb1 = -lm.sentenceProb(sentence1);
                return new ScoreResult(logProb1 / count1, logProb1, count1);
            default:
                throw new IllegalStateException("wrong xform");
        }
    }

    public static ArrayList<String> charsFromInput(String input) {
        return Lists.newArrayList(new CharAsStringIterable(input));
    }

    public static List<String> wordsFromInput(String input) {
        return WORD_SPLITTER.splitToList(input);
    }
}
