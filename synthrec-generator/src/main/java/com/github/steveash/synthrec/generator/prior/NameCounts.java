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

package com.github.steveash.synthrec.generator.prior;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.data.SampleEntry;
import com.github.steveash.synthrec.generator.enrich.NormalizerService;
import com.github.steveash.synthrec.name.EnglishWords;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.name.NameStopWords;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.string.BinnedDictSampler;
import com.github.steveash.synthrec.string.PatternReducer;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

/**
 * Owns association between the prior/counts for various name tag segments and the resource locations where
 * these reside
 * @author Steve Ash
 */
public class NameCounts {
    private static final Logger log = LoggerFactory.getLogger(NameCounts.class);

    public static final ImmutableMap<NamePart, String> TAG_TO_RESOURCE = ImmutableMap.<NamePart, String>builder()
            .put(NamePart.AKA, "names/prior/name.AKA.freq.clob")
            .put(NamePart.And, "names/prior/name.And.freq.clob")
            .put(NamePart.BabyAlias, "names/prior/name.BabyAlias.freq.clob")
            .put(NamePart.FirstInitial, "names/prior/name.FirstInitial.freq.clob")
            .put(NamePart.GivenTag, "names/prior/name.GivenTag.freq.clob")
            .put(NamePart.LastInitial, "names/prior/name.LastInitial.freq.clob")
            .put(NamePart.LineageTag, "names/prior/name.LineageTag.freq.clob")
            .put(NamePart.LongHonor, "names/prior/name.LongHonor.freq.clob")
            .put(NamePart.MiddleInitial, "names/prior/name.MiddleInitial.freq.clob")
            .put(NamePart.Prefix, "names/prior/name.Prefix.freq.clob")
            .put(NamePart.Skip, "names/prior/name.Skip.freq.clob")
            .put(NamePart.SuffixGenerational, "names/prior/name.SuffixGenerational.freq.clob")
            .put(NamePart.SuffixOther, "names/prior/name.SuffixOther.freq.clob")
            .put(NamePart.SurnameTag, "names/prior/name.SurnameTag.freq.clob")
            .build();

    public static CsvTable loadForTag(NamePart tag) {
        String resource = checkNotNull(TAG_TO_RESOURCE.get(tag), "cant find resource for tag", tag);
        return ReadWrite.loadCountTable(resource);
    }

    public static MutableMultinomial<String> loadMultinomialForTag(NamePart tag) {
        String resource = checkNotNull(TAG_TO_RESOURCE.get(tag), "cant find resource for tag", tag);
        return ReadWrite.loadCountTableAsMultinomial(resource, NormalizerService.STD_FUNC);
    }

    /**
     * Loads all of the words from the priors + all of the tokens from the translation tables
     * so that you can use this to replace long AAAA's in patterns
     * @return
     */
    public static BinnedDictSampler loadAllWords(NameStopWords words, EnglishWords englishWords) {
        Stopwatch watch = Stopwatch.createStarted();

        FluentIterable<SampleEntry<String>> allWords = FluentIterable.from(TAG_TO_RESOURCE.values())
                .transformAndConcat(rsr -> ReadWrite.loadCountTable(rsr).columnsAsStringIter(0))
                .append(words.getNormalStops())
                .append(englishWords.getFreqEnglish())
                .filter(PatternReducer.LETTERS::matchesAllOf)
                .transform(s -> new SampleEntry<>(s, 10));

        BinnedDictSampler sampler = new BinnedDictSampler(allWords, NormalizerService.STD_FUNC);
        watch.stop();
        log.info("Loaded all name words into the binned dictionary in " + watch.toString());
        return sampler;
    }
}
