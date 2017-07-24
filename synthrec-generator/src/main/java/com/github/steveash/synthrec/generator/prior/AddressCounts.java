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

import com.github.steveash.synthrec.address.AddressStopWords;
import com.github.steveash.synthrec.address.AddressTag;
import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.data.SampleEntry;
import com.github.steveash.synthrec.generator.enrich.NormalizerService;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.string.BinnedDictSampler;
import com.github.steveash.synthrec.string.PatternReducer;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

/**
 * Owns association between the prior/counts for various address segments and the resource locations where
 * these reside
 * @author Steve Ash
 */
public class AddressCounts {
    private static final Logger log = LoggerFactory.getLogger(AddressCounts.class);

    public static final ImmutableMap<AddressTag, String> TAG_TO_RESOURCE = ImmutableMap.<AddressTag, String>builder()
            .put(AddressTag.AptObject, "addr/prior/addr.APTBOXOBJ.freq.clob")
            .put(AddressTag.AptTag, "addr/prior/addr.APTBOXTAG.freq.clob")
            .put(AddressTag.CoTag, "addr/prior/addr.COTAG.freq.clob")
            .put(AddressTag.Designator, "addr/prior/addr.DESIGNATOR.freq.clob")
            .put(AddressTag.HighwayObject, "addr/prior/addr.HWYOBJ.freq.clob")
            .put(AddressTag.HighwayTag, "addr/prior/addr.HWYTAG.freq.clob")
            .put(AddressTag.PoBoxObject, "addr/prior/addr.POBOXOBJ.freq.clob")
            .put(AddressTag.PoBoxTag, "addr/prior/addr.POBOXTAG.freq.clob")
            .put(AddressTag.PostDirection, "addr/prior/addr.POSTDIR.freq.clob")
            .put(AddressTag.PreDirection, "addr/prior/addr.PREDIR.freq.clob")
            .put(AddressTag.RrBoxObject, "addr/prior/addr.RRBOXOBJ.freq.clob")
            .put(AddressTag.RrBoxTag, "addr/prior/addr.RRBOXTAG.freq.clob")
            .put(AddressTag.RrObject, "addr/prior/addr.RROBJ.freq.clob")
            .put(AddressTag.RrTag, "addr/prior/addr.RRTAG.freq.clob")
            .put(AddressTag.StreetNumber, "addr/prior/addr.STREETNO.freq.clob")
            .put(AddressTag.StreetName, "addr/prior/street-names.clob")
            .build();

    public static CsvTable loadForTag(AddressTag tag) {
        String resource = checkNotNull(TAG_TO_RESOURCE.get(tag), "cant find resource for tag", tag);
        return ReadWrite.loadCountTable(resource);
    }

    public static MutableMultinomial<String> loadMultinomialForTag(AddressTag tag) {
        String resource = checkNotNull(TAG_TO_RESOURCE.get(tag), "cant find resource for tag", tag);
        return ReadWrite.loadCountTableAsMultinomial(resource, NormalizerService.STD_FUNC);
    }

    /**
     * Loads all of the words from the priors + all of the tokens from the translation tables
     * so that you can use this to replace long AAAA's in patterns
     * @return
     */
    public static BinnedDictSampler loadAllWords(AddressStopWords addressStopWords) {
        Stopwatch watch = Stopwatch.createStarted();

        FluentIterable<SampleEntry<String>> allWords = FluentIterable.from(TAG_TO_RESOURCE.values())
                .transformAndConcat(rsr -> ReadWrite.loadCountTable(rsr).columnsAsStringIter(0))
                .append(addressStopWords.allStopWords())
                .filter(PatternReducer.LETTERS::matchesAllOf)
                .transform(s -> new SampleEntry<>(s, 10));

        BinnedDictSampler sampler = new BinnedDictSampler(allWords, NormalizerService.STD_FUNC);
        watch.stop();
        log.info("Loaded all address words into the binned dictionary in " + watch.toString());
        return sampler;
    }
}
