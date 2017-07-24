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
import java.time.format.DateTimeFormatter;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.gen.GenAssignment;
import com.github.steveash.synthrec.gen.GenContext;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.generator.demo.DobGenerator;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.ImmutableSet;

/**
 * Takes the age as input and generates the dob using the dob generator;
 * @author Steve Ash
 */
@LazyComponent
public class DobGenNode implements GenNode {

    @Resource private DobGenerator dobGenerator;

    @Override
    public boolean sample(RandomGenerator rand, GenAssignment assignment, GenContext context) {
        int age = (int) assignment.get(Constants.AGE_YEARS);
        LocalDate thisDob = dobGenerator.generate(rand, age);
        assignment.put(Constants.DOB_PARSED, thisDob);
        assignment.put(Constants.DOB, thisDob.format(DateTimeFormatter.ISO_LOCAL_DATE));
        return true;
    }

    @Override
    public Set<String> inputKeys() {
        return ImmutableSet.of(Constants.AGE_YEARS);
    }

    @Override
    public Set<String> outputKeys() {
        return ImmutableSet.of(Constants.DOB_PARSED, Constants.DOB);
    }
}
