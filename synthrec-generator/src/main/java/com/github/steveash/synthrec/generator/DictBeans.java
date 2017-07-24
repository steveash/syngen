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

package com.github.steveash.synthrec.generator;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.github.steveash.jg2p.model.CmuSyllabifierFactory;
import com.github.steveash.jg2p.syllchain.Syllabifier;
import com.github.steveash.synthrec.address.AddressStopWords;
import com.github.steveash.synthrec.data.TranslationTable;
import com.github.steveash.synthrec.generator.reducer.SimpleTokenReducer;
import com.github.steveash.synthrec.name.CensusGivenNames;
import com.github.steveash.synthrec.name.DefaultEnglishWords;
import com.github.steveash.synthrec.name.EnglishWords;
import com.github.steveash.synthrec.name.GivenNameByYear;
import com.github.steveash.synthrec.name.NameStopWords;
import com.github.steveash.synthrec.ssa.AgeDist;

/**
 * @author Steve Ash
 */
@Profile(DictBeans.DICTS)
@Lazy
@Import({ConfigBeans.class, RequiredDepBeans.class})
@Configuration
public class DictBeans {

    public static final String DICTS = "dicts";

    @Bean
    public Syllabifier syllabifier() {
        return CmuSyllabifierFactory.create();
    }

    @Bean
    public GivenNameByYear givenLookup(int basisYear) {
        return GivenNameByYear.makeWithBasis(basisYear);
    }

    @Bean
    public TranslationTable stateTranslationTable() {
        return TranslationTable.makeFromClasspathResource("addr/states.csv");
    }

    @Bean
    public CensusGivenNames censusGivenNames() {
        return CensusGivenNames.loadCensusDataFromCache();
    }

    @Bean
    public AgeDist ageDist() {
        return AgeDist.create();
    }

    @Bean
    public SimpleTokenReducer addressTokenReducer(AddressStopWords addressStopWords) {
        Set<String> publics = addressStopWords.allStopWords();
        return new SimpleTokenReducer(SimpleTokenReducer.MIN_RARE_COUNT, publics::contains);
    }

    @Bean
    public EnglishWords englishWords() {
        return DefaultEnglishWords.fromClasspath();
    }

    @Bean
    public SimpleTokenReducer nameTokenReducer(NameStopWords nameStopWords) {
        // we're just using the stop words here -- which excludes all of the english words because
        // we dont want to preserve english words here -- but we might want to preserve distrubition
        // of particular particles with punctuation (etc)
        return new SimpleTokenReducer(20, nameStopWords::isStopword);
    }
}
