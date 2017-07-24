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

import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.canonical.Normalizers;
import com.github.steveash.synthrec.canonical.SimpleNormalToken;
import com.github.steveash.synthrec.data.CsvTable;
import com.github.steveash.synthrec.data.CsvTable.Row;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.name.Gender;
import com.github.steveash.synthrec.name.gender.GenderTagger;
import com.github.steveash.synthrec.stat.Multinomial;
import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * A facade that represents the distribution of P( gender | name ) that uses an extensive
 * prior dictionary and falls back to the gender tagger if we get a dictionary miss
 * @author Steve Ash
 */
@LazyComponent
public class GenderGivenName {
    private static final Logger log = LoggerFactory.getLogger(GenderGivenName.class);

    private final GenderTagger genderTagger;
    private final Object2DoubleOpenHashMap<String> nameToMaleProb;
    private final LongAdder taggerHits = new LongAdder();

    @Autowired
    public GenderGivenName(GenderTagger genderTagger) {
        log.info("Loading the gender given name distribution");
        this.genderTagger = genderTagger;

        CsvTable table = ReadWrite.loadCountTable("names/prior/name.NameGender.clob");
        log.info("Reading from " + table);
        this.nameToMaleProb = new Object2DoubleOpenHashMap<>(table.estimateRowCount().orElse(350_000));
        this.nameToMaleProb.defaultReturnValue(Double.NaN);
        for (Row row : table) {
            // these are already normalized
            int male = row.getInt("MALE");
            int female = row.getInt("FEMALE");
            String normalName = row.getString("name");
            Preconditions.checkArgument(isNotBlank(normalName), "cant have a blank normal name");
            nameToMaleProb.put(Normalizers.interner().intern(normalName), ((double) male) / (male + female));
        }
    }

    public double probMale(String normalName) {
        double dictProb = nameToMaleProb.getDouble(normalName);
        if (Double.isNaN(dictProb)) {
            taggerHits.increment();
            Multinomial<Gender> result = genderTagger.predictGender(new SimpleNormalToken(normalName,
                    normalName
            ));
            return result.get(Gender.Male);
        }
        return dictProb;
    }

    public long taggerHits() {
        return taggerHits.longValue();
    }
}
