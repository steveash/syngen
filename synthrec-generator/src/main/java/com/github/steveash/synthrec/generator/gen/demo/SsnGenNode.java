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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.GenContext.ContextKey;
import com.github.steveash.synthrec.gen.InOutGenNode;
import com.github.steveash.synthrec.generator.demo.SsnGenerator;
import com.github.steveash.synthrec.generator.demo.SsnGenerator.InvalidSsaState;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.string.DigitReplacer;

/**
 * GenNode that takes an SSN Pattern and generates a new SSN value for it
 * This is very related to @see {@link com.github.steveash.synthrec.generator.feature.PatternFeature} so if
 * that changes dramatically then this might need to be updated
 * @author Steve Ash
 */
@LazyComponent
public class SsnGenNode extends InOutGenNode {

    private static final ContextKey<Set<String>> SSN_SEEN_KEY = new ContextKey<>(Constants.SSN + ".seen", HashSet::new);
    private static final int MAX_REJECT = 10;

    @Resource SsnGenerator ssnGenerator;

    public SsnGenNode() {
        super(Constants.SSN_PATTERN,
                Constants.DOB_PARSED,
                Constants.BIRTH_STATE,
                Constants.SSN
        );
    }

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        String pattern = (String) assignment.get(Constants.SSN_PATTERN);
        LocalDate dob = (LocalDate) assignment.get(Constants.DOB_PARSED);
        String birthState = (String) assignment.get(Constants.BIRTH_STATE);

        Set<String> ssns = context.get(SSN_SEEN_KEY);
        try {
            for (int i = 0; i < MAX_REJECT; i++) {
                String ssnVal = convert(rand, pattern, dob.getYear(), birthState);
                if (ssnVal.length() < 9) {
                    assignment.put(Constants.SSN, ssnVal);
                    return true;
                }
                // if its a nine digit ssn then we keep track of uniqueness
                if (ssns.add(ssnVal)) {
                    assignment.put(Constants.SSN, ssnVal);
                    return true;
                }
            }
        } catch (InvalidSsaState e) {
            return false; // try again
        }
        return false;
    }

    public String convert(RandomGenerator rand, String pattern, int birthYear, String birthState) {
        // pattern is based on @see PatternFeature which leaves a lot of punctuation
        int digits = DigitReplacer.DIGITS.countIn(pattern);
        if (digits == 0) {
            // just punctuation or some other pattern we dont care about
            return pattern;
        }

        String ssnValue = ssnGenerator.generate(rand, birthYear, birthState);
        // replace digits left to right, fill in the rest with random digits
        return DigitReplacer.replacePatternRtoL(rand, pattern, ssnValue);
    }
}
