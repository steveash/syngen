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

package com.github.steveash.synthrec.deident;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.collect.LazyConcurrentMap;
import com.github.steveash.synthrec.collect.Vocabulary;
import com.github.steveash.synthrec.sampling.ReservoirSet;
import com.github.steveash.synthrec.stat.RandUtil;
import com.github.steveash.synthrec.util.Action;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

/**
 * Takes a background distribution (a non-sensitive public source of given names for example) and does the following
 * 1) profile the background distribution values with an n-gram (character) based language model on the graphemes
 * 2) " " " " on the phonemes (using a given phonetic encoder)
 * 3) Use Jenks Breaks to split the distribution into 10 segments (maybe want to rethink this -- perhaps we could
 * learn a curve of X piece-wise lines that best approximate the curve and use the x intercepts there to split
 * this into different segments (how is this different from percentiles?)
 * 4) take an input distribution and any value that is not in the input distribution and occurrs less than k times -- we
 * need to replace it by a neighbor that is similar -- using some percentile/jenksbreaks-segment + similar complexity
 * in grapheme and phoneme space
 * @author Steve Ash
 */
public class KAnonDeidentifier<I, V> implements VocabDeidentifier<I> {
    private static final Logger log = LoggerFactory.getLogger(KAnonDeidentifier.class);
    private static final int MAX_BLOCK = 1_000;

    private final DeidentDistance<I, V> distance;
    private final Iterable<I> replaceCandidates;
    private final int minimumCount;
    private final RandomGenerator rand = RandUtil.threadLocalRand();

    private volatile Action deidentMarker = Action.NOOP;
    private volatile Action blockingMarker = Action.NOOP;
    private volatile Action firstPassMarker = Action.NOOP;

    public KAnonDeidentifier(DeidentDistance<I, V> distance,
            Iterable<I> replaceCandidates,
            int minimumCount
    ) {
        this.distance = distance;
        this.replaceCandidates = replaceCandidates;
        this.minimumCount = minimumCount;
    }

    public void setDeidentMarker(Action deidentMarker) {
        this.deidentMarker = deidentMarker;
    }

    public void setBlockingMarker(Action blockingMarker) {
        this.blockingMarker = blockingMarker;
    }

    public void setFirstPassMarker(Action firstPassMarker) {
        this.firstPassMarker = firstPassMarker;
    }

    @Override
    public void deidentify(Vocabulary<I> vocab, ToDoubleFunction<I> countForVocab, Observer observer) {
        Set<I> victims = Sets.newConcurrentHashSet();
        Map<I, ReservoirSet<I>> victimsToReplacements = LazyConcurrentMap.make(i -> new ReservoirSet<I>(MAX_BLOCK));

        collectAndBlockVictims(vocab,
                countForVocab,
                victims,
                victimsToReplacements
        );

        if (victims.isEmpty()) {
            log.info("Nothing needs to be deidentified");
            return;
        }

        firstPass(vocab, victims, victimsToReplacements, observer);
        victimsToReplacements.clear(); // clean up memory early
        secondPass(vocab, victims, observer);
    }

    private void secondPass(Vocabulary<I> vocab, Set<I> victimsLeft, Observer observer) {
        if (victimsLeft.isEmpty()) {
            return;
        }
        final int size = victimsLeft.size();
        int count = 0;
        // just do a reservoir sample for the rest
        List<I> sample = Lists.newArrayListWithCapacity(size);
        for (I candidate : replaceCandidates) {
            if (vocab.contains(candidate)) {
                continue;
            }
            count += 1;
            if (sample.size() < size) {
                sample.add(candidate);
            } else {
                int toReplace = rand.nextInt(count);
                if (toReplace < sample.size()) {
                    sample.set(toReplace, candidate);
                }
            }
        }

        int next = 0;
        Iterator<I> iter = victimsLeft.iterator();
        while (iter.hasNext() && next < sample.size()) {
            I victim = iter.next();
            I newValue = sample.get(next);
            iter.remove();
            observer.onSampleReplace(victim, newValue);
            vocab.updateIndexValue(vocab.getIndexFor(victim), newValue);
            next += 1;
        }

        log.info("Second pass deident " + size + ", randomly sampled " + next + ", " + victimsLeft.size() + " left");
    }

    private void firstPass(Vocabulary<I> vocab,
            Set<I> victims,
            Map<I, ReservoirSet<I>> victimsToReplacements,
            Observer observer
    ) {
        if (victims.isEmpty()) {
            return;
        }

        int totalVictimCount = victims.size();
        AtomicInteger replaceCount = new AtomicInteger();
        // now go through victim blocks and pick the best
        List<Map.Entry<I, ReservoirSet<I>>> entries = Ordering.natural()
                .onResultOf((Map.Entry<I, ReservoirSet<I>> e) -> e.getValue().getFinalSet().size())
                .sortedCopy(victimsToReplacements.entrySet());
        DescriptiveStatistics stats = new DescriptiveStatistics();
        entries.forEach(e -> stats.addValue(e.getValue().getTotalTried()));
        log.info("First pass needs to evaluate " + entries.size() + " blocks; size 1/25/50/75/95/99 tiles " +
                stats.getPercentile(1.0) + "/" +
                stats.getPercentile(25.0) + "/" +
                stats.getPercentile(50.0) + "/" +
                stats.getPercentile(75.0) + "/" +
                stats.getPercentile(95.0) + "/" +
                stats.getPercentile(99.0)
        );
        entries.parallelStream().forEach( entry -> {
            firstPassMarker.execute();
            I victim = entry.getKey();
            I bestReplacement = findBestReplacement(victim, entry.getValue().getFinalSet(), vocab);
            if (bestReplacement == null) {
                return;
            }
            replaceCount.incrementAndGet();
            victims.remove(victim);

            vocab.getLock().writeLock().lock();
            try {
                int index = vocab.getIndexFor(victim);
                observer.onBlockingReplace(victim, bestReplacement);
                vocab.updateIndexValue(index, bestReplacement);
            } finally {
                vocab.getLock().writeLock().unlock();
            }
        });

        log.info("First pass deident vocab " + vocab.size() + " needed to deident " + totalVictimCount +
                " victims, " + victimsToReplacements.keySet().size() + " had matching blocked options; replaced " +
                replaceCount + ", " + victims.size() + " left");
    }

    private I findBestReplacement(I victim, Collection<I> candidates, Vocabulary<I> currentVocab) {
        I best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        V victimVec = distance.makeVector(victim);
        for (I candidate : candidates) {
            if (currentVocab.contains(candidate)) {
                continue;
            }
            double dist = distance.distance(victimVec, distance.makeVector(candidate));
            if (best == null || dist < bestDistance) {
                best = candidate;
                bestDistance = dist;
            }
        }
        return best;
    }

    private void collectAndBlockVictims(Vocabulary<I> vocab,
            ToDoubleFunction<I> countForVocab,
            Set<I> victims,
            Map<I, ReservoirSet<I>> victimsToReplacements
    ) {
        log.info("running through all " + vocab.size() + " entries...");

        SetMultimap<String, I> keysToVictims = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        Streams.stream(vocab).parallel().forEach( value -> {
            deidentMarker.execute();
            if (distance.isPublicDomain(value)) {
                return;
            }
            if (countForVocab.applyAsDouble(value) >= minimumCount) {
                return;
            }
            victims.add(value);
            Set<String> keys = distance.blockingKeys(value);
            for (String key : keys) {
                keysToVictims.put(key, value);
            }
        });

        if (victims.isEmpty()) {
            return; // we dont need to block the replacements because there's nothing to deident
        }
        log.info("running through replace candidates to index them..");
        Streams.stream(replaceCandidates).parallel().forEach(candidate -> {
            blockingMarker.execute();
            Set<String> keys = distance.blockingKeys(candidate);
            for (String key : keys) {
                for (I victim : keysToVictims.get(key)) {
                    ReservoirSet<I> cands = victimsToReplacements.get(victim);
                    cands.tryAdd(rand, candidate);
                }
            }
        });
    }
}
