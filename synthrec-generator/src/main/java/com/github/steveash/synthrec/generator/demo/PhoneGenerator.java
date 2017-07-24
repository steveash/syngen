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

package com.github.steveash.synthrec.generator.demo;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collection;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.gen.TooManyRejectsSamplingException;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.socio.ZipData;
import com.github.steveash.synthrec.socio.ZipDataLookup;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.RandUtil;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.github.steveash.synthrec.string.PatternMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

/**
 * Generates a phone number for the given zip code; some % of phone numbers will be from
 * other zip codes, representing cell numbers for transplants that haven't changed them
 * The area codes are valid - but the exchanges are not necessarily valid;
 * numbers are just 10 digits -- no formatting is done
 * @author Steve Ash
 */
@LazyComponent
public class PhoneGenerator {

    private static final double PROB_OF_OOR_AREACODE = 0.001;

    // see wikipedia description of NANP: https://en.wikipedia.org/wiki/North_American_Numbering_Plan
    // numbers are always 10 digits
    private static final PatternMatcher badNumbers = PatternMatcher.builder()
            .matchRegex("(\\d{3})\\1\\d{4}")    // area code and exchange shouldnt be the same
            .matchRegex("\\d{4}11\\d+")         // N11 for exchange
            .matchRegex("\\d{3}55501\\d{2}")    // 555-01XX numbers are reserved for made up use
            .matchRegex("\\d{3}95[89]\\d{4}")   // exchanges 958 and 959 are reserved for testing
            .build();

    private static final int MAX_REJECT = 10_000;

    @Resource private ZipDataLookup zipDataLookup;

    private SamplingTable<String> areaCodePopulation; // global area codes + the population of the assoc metros
    private ImmutableMap<String, SamplingTable<String>> zipToAreas;

    @PostConstruct
    protected void setup() {
        // load zip -> area code association file
        MutableMultinomial<String> areaCodePop = new MutableMultinomial<>(-1);
        HashMultimap<String, String> zipToArea = HashMultimap.create();
        for (ZipData zipData : zipDataLookup.allZips()) {
            if (isBlank(zipData.getAreaCode()) || zipData.getAreaCode().equals("000")) {
                continue;
            }
            double pop = zipData.getEstimatedPopulation();
            if (pop <= 0) pop = 1.0;

            areaCodePop.add(zipData.getAreaCode(), pop);
            zipToArea.put(zipData.getZipcode(), zipData.getAreaCode());
        }
        areaCodePopulation = SamplingTable.createFromMultinomial(areaCodePop);

        Builder<String, SamplingTable<String>> builder = ImmutableMap.builder();
        for (Entry<String, Collection<String>> entry : zipToArea.asMap().entrySet()) {
            MutableMultinomial<String> multi = MutableMultinomial.createUnknownMax();
            for (String area : entry.getValue()) {
                multi.add(area, areaCodePop.get(area));
            }
            builder.put(entry.getKey(), SamplingTable.createFromMultinomial(multi));
        }
        zipToAreas = builder.build();
    }

    public String generate(RandomGenerator rand, String zip) {
        for (int i = 0; i < MAX_REJECT; i++) {
            String area = randAreaCode(rand, zip);
            String exch = randExchange(rand);
            String numb = randNumber(rand);
            String result = tryResult(area, exch, numb);
            if (result != null) return result;
        }
        throw new TooManyRejectsSamplingException();
    }

    public String generateGivenArea(RandomGenerator rand, String area) {
        Preconditions.checkArgument(area.length() == 3, "must submit a valid area");
        for (int i = 0; i < MAX_REJECT; i++) {
            String exch = randExchange(rand);
            String numb = randNumber(rand);
            String result = tryResult(area, exch, numb);
            if (result != null) return result;
        }
        throw new TooManyRejectsSamplingException();
    }

    private String tryResult(String area, String exch, String numb) {
        String result = area + exch + numb;
        Preconditions.checkState(result.length() == 10, "invalid phone");
        if (!badNumbers.matches(result)) {
            return result;
        }
        return null;
    }

    private String randNumber(RandomGenerator rand) {
        return String.format("%04d", rand.nextInt(10_000));
    }

    private String randAreaCode(RandomGenerator rand, String zip) {
        if (!zipToAreas.containsKey(zip) || rand.nextDouble() < PROB_OF_OOR_AREACODE) {
            return areaCodePopulation.sampleWeighted(rand);
        }
        SamplingTable<String> zipAreas = zipToAreas.get(zip);
        return zipAreas.sampleWeighted(rand);
    }

    private String randExchange(RandomGenerator rand) {
        int first = RandUtil.nextIntIn(rand, 2, 10);
        int second = RandUtil.nextIntIn(rand, 0, 100);
        return String.format("%d%02d", first, second);
    }
}
