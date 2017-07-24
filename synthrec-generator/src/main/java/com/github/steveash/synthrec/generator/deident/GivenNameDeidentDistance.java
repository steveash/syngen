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

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.github.steveash.guavate.Guavate;
import com.github.steveash.jg2p.syllchain.RuleSyllabifier;
import com.github.steveash.synthrec.name.GivenNameLookup;
import com.github.steveash.synthrec.name.culture.CultureDetector;
import com.github.steveash.synthrec.canonical.SimpleNormalToken;
import com.github.steveash.synthrec.deident.DeidentDistance;
import com.github.steveash.synthrec.name.gender.GenderTagger;
import com.github.steveash.synthrec.generator.deident.GivenNameDeidentDistance.NameSketch;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.Gender;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.phonetic.DoubleMetaphone;
import com.github.steveash.synthrec.phonetic.PhoneEncoder;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.string.GramIterable;
import com.github.steveash.synthrec.string.OptimalStringAlignment;

/**
 * Deidentifier for given names
 * @author Steve Ash
 */
@LazyComponent
public class GivenNameDeidentDistance implements DeidentDistance<String, NameSketch> {

    private static final int GRAM_SIZE = 4;

    @Resource private GivenNameLookup givenNameLookup;  // agg of all public names we know about
    @Resource private GenderTagger genderTagger;
    @Resource private CultureDetector cultureDetector;
    @Resource private CommonNamePublicRule commonNamePublicRule;
    @Resource private PhoneEncoder phoneEncoder;

    public static class NameSketch {
        final String original;
        final String phonetic;
        final int syllableCount;
        final double percentile;
        final Multinomial<String> culture;
        final Multinomial<Gender> gender;

        public NameSketch(String original,
                String phonetic,
                int syllableCount,
                double percentile,
                Multinomial<String> culture,
                Multinomial<Gender> gender
        ) {
            this.original = original;
            this.phonetic = phonetic;
            this.syllableCount = syllableCount;
            this.percentile = percentile;
            this.culture = culture;
            this.gender = gender;
        }

        @Override
        public String toString() {
            return "NameSketch{" +
                    "original='" + original + '\'' +
                    ", phonetic='" + phonetic + '\'' +
                    ", syllableCount=" + syllableCount +
                    ", percentile=" + percentile +
                    ", culture=" + culture +
                    ", gender=" + gender +
                    '}';
        }
    }

    public Iterable<String> allNames() {
        return givenNameLookup.allNames();
    }

    @Override
    public boolean isPublicDomain(String input) {
        return commonNamePublicRule.isPublicDomain(input);
    }

    @Override
    public NameSketch makeVector(String origInput) {
        String normalized = Names.normalize(origInput);
        SimpleNormalToken sni = new SimpleNormalToken(origInput, normalized);
        return new NameSketch(
                normalized,
                phoneEncoder.encode(normalized),
                RuleSyllabifier.syllable(normalized),
                0.0,
                cultureDetector.detectSingleToken(sni),
                genderTagger.predictGender(sni)
        );
    }

    @Override
    public double distance(NameSketch comp1, NameSketch comp2
    ) {
        double sum = 0;
        sum += 1.00 * OptimalStringAlignment.editDistanceNormalzied(comp1.original, comp2.original);
        sum += 1.00 * OptimalStringAlignment.editDistanceNormalzied(comp1.phonetic, comp2.phonetic);
        sum += 0.25 * syllDist(comp1.syllableCount, comp2.syllableCount);
        sum += 0.50 * comp1.culture.jensonShannonDivergence(comp2.culture);
        sum += 0.25 * comp1.gender.jensonShannonDivergence(comp2.gender);
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
