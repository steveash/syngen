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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.github.steveash.synthrec.address.AddressStopWords;
import com.github.steveash.synthrec.address.AddressStopWordsAdapter;
import com.github.steveash.synthrec.data.ReadWrite;
import com.github.steveash.synthrec.date.DateParser;
import com.github.steveash.synthrec.name.CensusGivenNames;
import com.github.steveash.synthrec.name.DefaultGivenNameLookup;
import com.github.steveash.synthrec.name.DefaultSurnameLookup;
import com.github.steveash.synthrec.name.GivenNameLookup;
import com.github.steveash.synthrec.name.NameStopWords;
import com.github.steveash.synthrec.name.NameStopWordsAdapter;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.name.SurnameLookup;
import com.github.steveash.synthrec.name.gender.DefaultGenderTagger;
import com.github.steveash.synthrec.name.gender.GenderTagger;
import com.github.steveash.synthrec.phonetic.DoubleMetaphone;
import com.github.steveash.synthrec.phonetic.PhoneEncoder;
import com.google.common.collect.ImmutableSet;

/**
 * These are the default implementations of extensible components that we provide in the open-source version
 * @author Steve Ash
 */
@Lazy
@Configuration
public class OptionalDepBeans {

    // a very limited default set of stop words really just as an example; you should use your own comprehensive
    // list
    @Bean
    @ConditionalOnMissingBean(AddressStopWords.class)
    public AddressStopWords defaultAddressStopWords() {
        ImmutableSet<String> stops = ReadWrite.linesFrom("addr/defaultStopWords.csv")
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(ImmutableSet.toImmutableSet());
        return new AddressStopWordsAdapter(stops);
    }

    // a very limited set of name stop words, highly recommend to use your own curated set of multicultural
    // name tokens/stops
    @Bean
    @ConditionalOnMissingBean(NameStopWords.class)
    public NameStopWords defaultNameStopWords() {
        ImmutableSet<String> stops = ReadWrite.linesFrom("names/defaultStopWords.csv")
                .filter(StringUtils::isNotBlank)
                .map(Names::normalizeIntern)
                .collect(ImmutableSet.toImmutableSet());
        return new NameStopWordsAdapter(stops);
    }

    // the default given name lookup is just the SSA and census names; you should provide your own
    @ConditionalOnMissingBean(GivenNameLookup.class)
    @Bean
    public GivenNameLookup defaultGivenNameLookup() {
        return DefaultGivenNameLookup.load();
    }

    @ConditionalOnMissingBean(SurnameLookup.class)
    @Bean
    public SurnameLookup defaultSurnameLookup() {
        return DefaultSurnameLookup.load();
    }

    // default gender tagger is only using the census data and not recommended for
    // high quality use (use your own)
    @ConditionalOnMissingBean(GenderTagger.class)
    @Bean
    public GenderTagger defaultGenderTagger(CensusGivenNames censusGivenNames) {
        return new DefaultGenderTagger(censusGivenNames);
    }

    // if you have licensed triple metaphone i highly recommend overriding this and providing it
    @Bean
    @ConditionalOnMissingBean(PhoneEncoder.class)
    public PhoneEncoder defaultPhoneEncoder() {
        return DoubleMetaphone.INSTANCE;
    }

    // if you have a better date parser you will probably want to use it since hospital extracts
    // have very noisy date formats and this doesn't take a lot of options
    @Bean
    @ConditionalOnMissingBean(DateParser.class)
    public DateParser defaultDateParser() {
        return (s) -> {
            try {
                LocalDate dt = LocalDate.parse(s);
                return dt.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                // if we can't handle the format then return empty; you should provide your own more
                // extensible parser to handle a wider range of dates if you want
                return "";
            }
        };
    }
}
