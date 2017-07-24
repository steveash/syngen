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

package com.github.steveash.synthrec.sample;

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
 * Spring configuration that needs to be filled out with references to your proprietary parsers, datasets,
 * etc. In order to run the synthrec process, you need to provide impls for each of these
 * @author Steve Ash
 */
@Lazy
@Import(OptionalBeans.class)
@Configuration
public class RequiredBeans {

    @Bean
    public CultureDetector cultureDetector() {
        return null; // TODO replace with your impl
    }

    @Bean
    public NameTagger nameTagger() {
        return null; // TODO replace with your impl
    }

    @Bean
    public NameChunker nameChunker() {
        return null; // TODO replace with your impl
    }

    @Bean
    public RawAddressParser rawAddressParser() {
        return null; // TODO replace with your impl
    }

}
