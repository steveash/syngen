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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import com.github.steveash.synthrec.address.RawAddressParser;
import com.github.steveash.synthrec.name.NameChunker;
import com.github.steveash.synthrec.name.NameTagger;
import com.github.steveash.synthrec.name.culture.CultureDetector;
import com.github.steveash.synthrec.socio.ZipDataLookup;

/**
 * all of the depenendencies that are required to be satisfied in order to use ths synthrec
 * toolkit; some of these have reasonable defaults that we provide; others you need to obtain
 * @author Steve Ash
 */
@Lazy
@Import(OptionalDepBeans.class)
@Configuration
public class RequiredDepBeans {

    @ConditionalOnMissingBean(CultureDetector.class)
    @Bean
    public CultureDetector defaultCultureDetector() {
        throw MissingComponentException.makeForComponent("Name Culture Detector", "cultureDetector");
    }

    @ConditionalOnMissingBean(NameTagger.class)
    @Bean
    public NameTagger defaultNameTagger() {
        throw MissingComponentException.makeForComponent("Personal Name Parser", "nameTagger");
    }

    @ConditionalOnMissingBean(NameChunker.class)
    @Bean
    public NameChunker defaultNameChunker() {
        throw MissingComponentException.makeForComponent("Personal Name Chunker", "nameChunker");
    }

    @Bean
    @ConditionalOnMissingBean(RawAddressParser.class)
    public RawAddressParser defaultRawAddressParser() {
        throw MissingComponentException.makeForComponent("Address Parser", "addressParser");
    }

    @Bean
    @ConditionalOnMissingBean(ZipDataLookup.class)
    public ZipDataLookup defaultZipDataLookup() {
        throw MissingComponentException.makeForComponent("Zip Data Lookup", "zipDataLookup");
    }

}
