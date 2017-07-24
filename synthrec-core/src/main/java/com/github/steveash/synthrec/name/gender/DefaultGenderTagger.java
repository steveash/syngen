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

package com.github.steveash.synthrec.name.gender;

import com.github.steveash.synthrec.canonical.NormalToken;
import com.github.steveash.synthrec.name.CensusGivenNames;
import com.github.steveash.synthrec.name.Gender;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;

/**
 * This is the default that only uses the census list to predict gender. This is a
 * very naive way to predict gender; we recommend that you use a more comprehensive
 * gender + some fallback classifier for OOV names (which is what we did for the paper
 * but unfortunately cannot release it as open source)
 * @author Steve Ash
 */
public class DefaultGenderTagger implements GenderTagger {

    public static final double THRESH = 0.75; // above this and we give it gender affinity
    public static final double CONTRA_THRESH = 1.0 - THRESH; // above this and we give it gender affinity

    private final CensusGivenNames censusGivenNames;

    public DefaultGenderTagger(CensusGivenNames censusGivenNames) {this.censusGivenNames = censusGivenNames;}

    @Override
    public Multinomial<Gender> predictGender(NormalToken input) {
        double female = censusGivenNames.femaleCountFor(input.getNormalToken());
        double male = censusGivenNames.maleCountFor(input.getNormalToken());
        if (female <= 0 && male <= 0) {
            return Multinomial.empty();
        }
        MutableMultinomial<Gender> multi = new MutableMultinomial<>(Gender.values().length);
        if (female > 0) {
            multi.add(Gender.Female, female);
        }
        if (male > 0) {
            multi.add(Gender.Male, male);
        }
        return multi.normalize();
    }

    @Override
    public Gender tagGender(NormalToken input) {
        Multinomial<Gender> predict = predictGender(input);
        if (predict.isEmpty()) {
            return Gender.Unknown;
        }
        double maleValue = predict.get(Gender.Male);
        if (maleValue > THRESH) {
            return Gender.Male;
        } else if (maleValue < CONTRA_THRESH) {
            return Gender.Female;
        }
        return Gender.Both;
    }
}
