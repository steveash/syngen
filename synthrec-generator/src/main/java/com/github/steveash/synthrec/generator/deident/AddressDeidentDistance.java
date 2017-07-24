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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.address.AddressStopWords;
import com.github.steveash.synthrec.address.AddressTag;
import com.github.steveash.guavate.Guavate;
import com.github.steveash.jg2p.syllchain.RuleSyllabifier;
import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.deident.DeidentDistance;
import com.github.steveash.synthrec.generator.deident.AddressDeidentDistance.AddressSketch;
import com.github.steveash.synthrec.generator.enrich.NormalizerService;
import com.github.steveash.synthrec.generator.prior.AddressCounts;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.phonetic.DoubleMetaphone;
import com.github.steveash.synthrec.phonetic.PhoneEncoder;
import com.github.steveash.synthrec.string.GramIterable;
import com.github.steveash.synthrec.string.OptimalStringAlignment;
import com.github.steveash.synthrec.string.PatternReducer;
import com.github.steveash.synthrec.string.StringBinner;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;

/**
 * Deident distance for finding address component tokens
 * @author Steve Ash
 */
@LazyComponent
public class AddressDeidentDistance implements DeidentDistance<String, AddressSketch> {
    private static final Logger log = LoggerFactory.getLogger(AddressDeidentDistance.class);

    private static final Pattern PUBLICS = Pattern.compile(
            "(?:(?:\\w{1,3}(?:-|/)?)?\\d{1,4}|\\d{1,4}(?:-|/)?(?:\\w{1,3})?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final CharMatcher PUNCS = CharMatcher.anyOf("!@#$%^&*(){}-_=+[]\\|/.,<>;:'\"`~");
    private static final Pattern ALL_SAME_NUMBER = Pattern.compile("(\\d)\\1{2,}");
    private static final int GRAM_SIZE = 4;

//    @Resource private Syllabifier syllabifier;

    @Resource private PhoneEncoder phoneEncoder;
    @Resource private AddressStopWords addressStopWords;

    private Set<String> allPublicAddressTokens;
    private Set<String> addressStopTokens;

    @PostConstruct
    protected void setup() {
        Stopwatch watch = Stopwatch.createStarted();
        log.info("Reading street address tokens...");
        addressStopTokens = addressStopWords.allStopWords();

        Splitter splitter = Splitter.on(CharMatcher.whitespace());
        HashSet<String> tokens = Sets.newHashSet();

        int count = 0;
        for (AddressTag addressTag : AddressCounts.TAG_TO_RESOURCE.keySet()) {
            CsvTable table = AddressCounts.loadForTag(addressTag);
            for (Row row : table) {
                String street = row.getString(0);
                for (String token : splitter.split(street)) {
                    if (isRuleBasedPublic(token)) {
                        continue;
                    }
                    tokens.add(NormalizerService.STD_FUNC.apply(token));
                }
                count += 1;
            }
        }
        watch.stop();
        this.allPublicAddressTokens = tokens;
        log.info("... finished reading " + count + " address lines " + tokens.size() + " tokens in " + watch.toString());
    }

    public static class AddressSketch {
        final String original;
        final String phonetic;
        final int syllableCount;

        public AddressSketch(String original,
                String phonetic,
                int syllableCount
        ) {
            this.original = original;
            this.phonetic = phonetic;
            this.syllableCount = syllableCount;
        }
    }

    public Set<String> allPublicTokens() {
        return allPublicAddressTokens;
    }

    @Override
    public AddressSketch makeVector(String input) {
        return new AddressSketch(
                input,
                phoneEncoder.encode(input),
                RuleSyllabifier.syllable(input)
        );
    }

    @Override
    public boolean isPublicDomain(String input) {
        if (isRuleBasedPublic(input) ||
                PUBLICS.matcher(input).matches() ||
                addressStopTokens.contains(input.toUpperCase()) ||
                allPublicAddressTokens.contains(input)) {
            return true;
        }
        if (PUNCS.matches(input.charAt(0)) && allPublicAddressTokens.contains(input.substring(1))) {
            return true;
        }
        if (PUNCS.matches(input.charAt(input.length() - 1)) && allPublicAddressTokens.contains(input.substring(0,
                input.length() - 1
        ))) {
            return true;
        }
        return false;
    }

    private static boolean isRuleBasedPublic(String input) {
        return (input.length() <= 2 ||
                Constants.MISSING.equals(input) ||
                StringBinner.isTagged(input) ||
                PatternReducer.isTagged(input) ||
                ALL_SAME_NUMBER.matcher(input).matches());
    }

    @Override
    public double distance(AddressSketch comp1, AddressSketch comp2) {
        double sum = 0;
        sum += 1.00 * OptimalStringAlignment.editDistanceNormalzied(comp1.original, comp2.original);
        sum += 1.00 * OptimalStringAlignment.editDistanceNormalzied(comp1.phonetic, comp2.phonetic);
        sum += 0.25 * syllDist(comp1.syllableCount, comp2.syllableCount);
        return sum;
    }

    private double syllDist(int count1, int count2) {
        int dist = Math.abs(count1 - count2);
        dist = Math.min(dist, 5);
        return ((double) dist) / 5.0;
    }

    @Override
    public Set<String> blockingKeys(String input) {
        return DoubleMetaphone.INSTANCE.encodeAllVariations(Names.normalize(input)).stream()
                .flatMap(k -> Guavate.stream(GramIterable.gramsOrDefault(k, GRAM_SIZE)))
                .collect(Collectors.toSet());
    }
}
