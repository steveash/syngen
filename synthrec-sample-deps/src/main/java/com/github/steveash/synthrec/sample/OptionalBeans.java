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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * This contains commented out stubs for optional components that you might want to use; if you
 * have better versions of these then you should use them
 * @author Steve Ash
 */
@Lazy
@Configuration
public class OptionalBeans {

    /*
    Commenting these out because there are defaults available in the synthrec-generator
    project that you can use; this is just showing an example of how to define your own
    override if you want to provide versions of any of the below.

    @Bean
    public AddressStopWords addressStopWords() {
        return null; // TODO replace with your impl
    }

    @Bean
    public NameStopWords nameStopWords() {
        return null; // TODO replace with your impl
    }

    @Bean
    public GivenNameLookup givenNameLookup() {
        return null; // TODO replace with your impl
    }

    @Bean
    public SurnameLookup surnameLookup() {
        return null; // TODO replace with your impl
    }

    @Bean
    public GenderTagger genderTagger() {
        return null; // TODO replace with your impl
    }

    @Bean
    public PhoneEncoder phoneEncoder() {
        return null; // TODO replace with your impl
    }

    @Bean
    public DateParser dateParser() {
        return null; // TODO replace with your impl
    }

    @Bean
    public ZipDataLookup zipDataLookup() {
        return null; // TODO replace with your impl
    }

     */
}
