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

package com.github.steveash.synthrec.generator.gen.demo;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.InOutGenNode;
import com.github.steveash.synthrec.generator.feature.CityBinFeature;
import com.github.steveash.synthrec.generator.feature.CityBinFeature.CityBin;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Takes the current state in which they live and generates what state they were born in using
 * simple census stats about how often people move away
 * @author Steve Ash
 */
@LazyComponent
public class BirthStateGenNode extends InOutGenNode {

    private static final int MAX_REJECT = 1_000;
    @Resource private CityBinFeature cityBinFeature;

    private Supplier<SamplingTable<String>> stateSampler;

    public BirthStateGenNode() {
        super(Constants.AGE_YEARS,
                Constants.ADDRESS_STATE,
                Constants.ADDRESS_CITY_BIN,
                Constants.BIRTH_STATE);
    }

    @PostConstruct
    protected void setup() {
        stateSampler = Suppliers.memoize( () -> {
            MutableMultinomial<String> states = cityBinFeature.statePopulations().copy();
            states.remove("AE");
            states.remove("AA");
            return SamplingTable.createFromMultinomial(states);
        });
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context
    ) {
        String state = (String) assignment.get(Constants.ADDRESS_STATE);
        CityBin cityBin = (CityBin) assignment.get(Constants.ADDRESS_CITY_BIN);
        int ageYears = (int) assignment.get(Constants.AGE_YEARS);

        double moveProb = calcMoveProb(cityBin, ageYears);
        if (rand.nextDouble() > moveProb) {
            // we didnt move so you were born here
            assignment.put(Constants.BIRTH_STATE, state);
            return true;
        } else {
            // we moved so lets pick a state where we came from
            for (int i = 0; i < MAX_REJECT; i++) {
                String fromState = stateSampler.get().sampleWeighted(rand);
                if (!state.equalsIgnoreCase(fromState)) {
                    assignment.put(Constants.BIRTH_STATE, fromState);
                    return true;
                }
            }

        }
        return false; // we couldnt sample to get a new state that wasn't our current state
    }

    public double calcMoveProb(CityBin cityBin, int ageYears) {
        if (ageYears <= 3) {
            return 0.001; // children dont move often
        }
        if (cityBin == CityBin.Small) {
            // people don't leave small cities as often and especially not before they are adults
            if (ageYears <= 18) {
                return 0.001;
            }
            return 0.15; // adults lets say have a 15% chance of getting out
        }

        // bigger cities
        if (ageYears <= 18) {
            return 0.03;
        }
        if (ageYears <= 30) {
            return 0.25;
        }
        return 0.60;
    }
}
