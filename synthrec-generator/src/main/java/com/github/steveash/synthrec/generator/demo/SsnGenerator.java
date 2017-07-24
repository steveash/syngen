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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.data.TranslationTable;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.util.MoreMath;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Generates SSN numbers that are reasonably similar to real SSA structure
 * @author Steve Ash
 */
@LazyComponent
public class SsnGenerator {
    private static final Pattern pattern = Pattern.compile("(\\d+)\\s*(?:[–-]\\s*(\\d+))?\\s+(.+)");

    @Resource private TranslationTable stateTranslationTable;
    @Resource private int basisYear;
    private Map<String, Range<Integer>> stateToAreaIds; // state -> area id

    private final ImmutableSet<String> skips = ImmutableSet.of(
            "123-45-6789",
            "987-65-4321",
            "012-34-5678",
            "001-23-4567"
    );

    public static class InvalidSsaState extends RuntimeException {
        public InvalidSsaState() {
        }

        public InvalidSsaState(String message) {
            super(message);
        }
    }

    @PostConstruct
    protected void setup() {
        stateToAreaIds = Maps.newHashMap();
        ReadWrite.linesFrom("socio/ssa-old-area.txt").forEach(line -> {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Cant parse SSA area file for line: " + line);
            }
            Range<Integer> range;
            String from = matcher.group(1);
            String to = matcher.group(2);
            Preconditions.checkState(isNotBlank(from));
            if (isNotBlank(to)) {
                range = Range.closed(Integer.valueOf(from), Integer.valueOf(to));
            } else {
                range = Range.singleton(Integer.valueOf(from));
            }
            String stateString = matcher.group(3);
            String state = stateTranslationTable.translateOrNull(stateString.trim().toUpperCase());
            if (state == null) {
                throw new IllegalArgumentException("Unknown SSA area state string " + stateString + " from " + line);
            }
            stateToAreaIds.put(state, range);
        });

        SetView<String> missing = Sets.difference(stateTranslationTable.distinctTargetValues(),
                stateToAreaIds.keySet()
        );
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing SSA area definitions for states: " + missing);
        }
    }

    /**
     * @param rand
     * @param birthYear the birth year of the person -- governs new vs old generation style
     * @param birthState state string as if it had gone through AddressTranslationTable#state
     * @return
     */
    public String generate(RandomGenerator rand, int birthYear, String birthState) {

        while (true) {
            String ssn;
            if (birthYear >= 2011) {
                ssn = generateNewStyle(rand);
            } else {
                ssn = generateOldStyle(rand, birthYear, birthState);
            }
            if (skips.contains(ssn)) continue;

            return ssn;
        }
    }

    private String generateNewStyle(RandomGenerator rand) {
        int area;
        while (true) {
            area = rand.nextInt(900); // anything >= 900 are tax ids
            if (area == 0 || area == 666) continue;
            break;
        }
        int groupId = 1 + rand.nextInt(99);
        int serial;
        while (true) {
            serial = rand.nextInt(10000);
            if (serial == 0 || serial == 9999) continue;
            break;
        }
        return String.format("%03d-%02d-%04d", area, groupId, serial);
    }

    private String generateOldStyle(RandomGenerator rand, int birthYear, String birthState) {
        Range<Integer> range = stateToAreaIds.get(birthState);
        if (range == null) {
            throw new InvalidSsaState("Cant find SSA area state " + birthState);
        }
        int area;
        int spread = range.upperEndpoint() - range.lowerEndpoint() + 1;
        if (spread == 1) {
            area = range.upperEndpoint();
        } else {
            int age = MoreMath.minMax(basisYear - birthYear, 0, 100);
            int mean = (int) (spread * MoreMath.percSpreadOf(age, 0, 100));
            int toAdd = (int) ((rand.nextGaussian() * spread) + mean);
            area = MoreMath.minMax(range.lowerEndpoint() + toAdd, range.lowerEndpoint(), range.upperEndpoint());
        }
        int groupId = 1 + rand.nextInt(99);
        int serial;
        while (true) {
            serial = rand.nextInt(10000);
            if (serial == 0 || serial == 9999) continue;
            break;
        }
        return String.format("%03d-%02d-%04d", area, groupId, serial);
    }
}
