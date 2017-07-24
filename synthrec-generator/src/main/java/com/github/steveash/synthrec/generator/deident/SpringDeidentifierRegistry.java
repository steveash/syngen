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

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.deident.DeidentifierRegistry;
import com.github.steveash.synthrec.deident.KAnonDeidentifier;
import com.github.steveash.synthrec.deident.RemapDeidentifier;
import com.github.steveash.synthrec.deident.VocabDeidentifier;
import com.github.steveash.synthrec.generator.spring.LazyComponent;

/**
 * Owns the strategy for constructing VocabDie for all demographics fields; if you add a new
 * field that you want to deidentify this strategy needs to be updated to support it;
 * collaborates with the deidentRecordService
 * @author Steve Ash
 */
@LazyComponent
public class SpringDeidentifierRegistry implements DeidentifierRegistry {

    @Resource private GivenNameDeidentDistance givenNameDeidentDistance;
    @Resource private FamilyNameDeidentDistance familyNameDeidentDistance;
    @Resource private AddressDeidentDistance addressDeidentDistance;
    @Resource private PhoneDeidentifier phoneDeidentifier;

    @Value("${synthrec.gen.records.min-count-anonymity}") private int minCountAnon;

    private KAnonDeidentifier<String, GivenNameDeidentDistance.NameSketch> givenNameDeident;
    private KAnonDeidentifier<String, FamilyNameDeidentDistance.NameSketch> familyNameDeident;
    private KAnonDeidentifier<String, AddressDeidentDistance.AddressSketch> addressDeident;
    private RemapDeidentifier<String> phoneDeident;

    @PostConstruct
    protected void setup() {
        givenNameDeident = new KAnonDeidentifier<>(givenNameDeidentDistance,
                givenNameDeidentDistance.allNames(), minCountAnon
        );
        familyNameDeident = new KAnonDeidentifier<>(familyNameDeidentDistance,
                familyNameDeidentDistance.allNames(), minCountAnon
        );
        addressDeident = new KAnonDeidentifier<>(addressDeidentDistance,
                addressDeidentDistance.allPublicTokens(), minCountAnon
        );
        phoneDeident = new RemapDeidentifier<>(phoneDeidentifier);
    }

    @Override
    public <I> VocabDeidentifier<I> deidentifierFor(String fieldName, @Nullable String subFieldName) {
        switch (fieldName) {
            case Constants.GIVEN_NAME:
            case Constants.GIVEN_NAMEISH:
            case Constants.MIDDLE_NAME:
            case Constants.GIVEN_NAME_STRUCT:
                return (VocabDeidentifier<I>) givenNameDeident;
            case Constants.FAMILY_NAME:
            case Constants.FAMILY_NAMEISH:
            case Constants.FAMILY_NAME_STRUCT:
                return (VocabDeidentifier<I>) familyNameDeident;
            case Constants.ADDRESS_STREET:
            case Constants.ADDRESS_STREET_STRUCT:
                return (VocabDeidentifier<I>) addressDeident;
            case Constants.PHONE:
                return (VocabDeidentifier<I>) phoneDeident;
            default:
                throw new IllegalArgumentException("Dont know how to deident for " + fieldName);
        }
    }
}
