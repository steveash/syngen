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

package com.github.steveash.synthrec.generator.deident;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.collect.Vocabulary;
import com.github.steveash.synthrec.deident.KAnonDeidentifier;
import com.github.steveash.synthrec.deident.SimpleEditDistance;
import com.github.steveash.synthrec.deident.VocabDeidentifier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * @author Steve Ash
 */
public class KAnonDeidentifierTest {
    private static final Logger log = LoggerFactory.getLogger(KAnonDeidentifierTest.class);
    private VocabDeidentifier<String> subst;
    private Map<String, Integer> prior;
    private Vocabulary<String> vocab;
    private Consumer<String> pw = log::info;

    @Before
    public void setUp() throws Exception {
        prior = Maps.newHashMap();
        prior.put("S_AAA", 1024);
        prior.put("S_BBB", 512);
        prior.put("S_CCC", 256);
        prior.put("S_DDD", 128);
        prior.put("S_EEE", 64);
        prior.put("S_FFF", 32);
        prior.put("S_GGG", 16);
        prior.put("S_HHH", 8);
        prior.put("S_III", 4);
        prior.put("S_JJJ", 2);
        vocab = new Vocabulary<>();
        subst = new KAnonDeidentifier<>(SimpleEditDistance.INSTANCE, prior.keySet(), 50);
    }

    @Test
    public void shouldNoOpAllAboveK() throws Exception {
        prior.keySet().forEach(vocab::putIfAbsent);
        log.info("Before size " + vocab.size());
        vocab.printTo(pw);
        Vocabulary<String> before = new Vocabulary<>(vocab);
        subst.deidentify(vocab, s -> 150.0, VocabDeidentifier.NULL_OBSERVER);
        log.info("After");
        vocab.printTo(pw);
        assertTrue(vocab.equalTo(before));
    }

    @Test
    public void shouldNoOpNothing() throws Exception {
        log.info("Before size " + vocab.size());
        vocab.printTo(pw);
        Vocabulary<String> before = new Vocabulary<>(vocab);
        subst.deidentify(vocab, s -> (double) prior.get(s), VocabDeidentifier.NULL_OBSERVER);
        log.info("After");
        vocab.printTo(pw);
        assertTrue(vocab.equalTo(before));
    }

    @Test
    public void shouldReplaceClosest() throws Exception {
        Map<String, Double> inp = Maps.newHashMap();
        inp.put("S_AAA", 1024.0);
        inp.put("S_BBB", 512.0);
        inp.put("S_FFF", 32.0);
        inp.put("S_GHH", 16.0);
        inp.put("S_HGG", 8.0);
        inp.put("S_ZZZZZZ", 4.0);
        Vocabulary<String> vocab = new Vocabulary<>();
        inp.keySet().forEach(vocab::putIfAbsent);
        log.info("Before");
        vocab.printTo(pw);
        subst.deidentify(vocab, inp::get, VocabDeidentifier.NULL_OBSERVER);
        log.info("After");
        vocab.printTo(pw);
        assertNoMixed(vocab);
    }

    private void assertNoMixed(Vocabulary<String> vocab) {
        for (String entry : vocab) {
            char c = entry.charAt(2);
            boolean mixed = false;
            for (int i = 3; i < entry.length(); i++) {
                if (entry.charAt(i) != c) {
                    mixed = true;
                }
            }
            assertFalse(mixed);
        }
    }

    @Test
    public void shouldResortToRandomIfNeeded() throws Exception {
        subst = new KAnonDeidentifier<>(new SimpleEditDistance() {
            @Override
            public Set<String> blockingKeys(String input) {
                return ImmutableSet.of();
            }
        }, prior.keySet(), 50);

        Map<String, Double> inp = Maps.newHashMap();
        inp.put("S_AAA", 1024.0);
        inp.put("S_BBB", 512.0);
        inp.put("S_FFF", 32.0);
        inp.put("S_GHH", 16.0);
        inp.put("S_HGG", 8.0);
        inp.put("S_ZZZZZZ", 4.0);
        Vocabulary<String> vocab = new Vocabulary<>();
        inp.keySet().forEach(vocab::putIfAbsent);
        log.info("Before");
        vocab.printTo(pw);
        subst.deidentify(vocab, inp::get, VocabDeidentifier.NULL_OBSERVER);
        log.info("After");
        vocab.printTo(pw);
        assertNoMixed(vocab);
    }
}