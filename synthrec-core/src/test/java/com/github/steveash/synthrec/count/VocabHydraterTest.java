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

package com.github.steveash.synthrec.count;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.domain.FieldSketch;

/**
 * @author Steve Ash
 */
public class VocabHydraterTest {
    private static final Logger log = LoggerFactory.getLogger(VocabHydraterTest.class);

    private DistribVocabRegistry registry;
    private VocabHydrater hydrater;

    @Before
    public void setUp() throws Exception {
        registry = new DistribVocabRegistry();
        hydrater = new VocabHydrater(registry);
    }

    @Test
    public void shouldRoundTripHierarch() throws Exception {
        FieldSketch nameInput = FieldSketch.builder()
                .addPlaceholder("FN", "STEVE")
                .addPlaceholder("LN", "ASH")
                .addLiteral("SX", "JR")
                .build();
        FieldSketch addrInput = FieldSketch.builder()
                .addPlaceholder("SN", "2301")
                .addPlaceholder("ST", "FOREST HILL")
                .addLiteral("CITY", "GTOWN")
                .build();
        AssignmentInstance assignInput = AssignmentInstance.make(
                "NAME", nameInput,
                "ADDR", addrInput,
                "SEX", "M"
        );
        log.info("Starting registry: \n" + registry.toBigString());
        DehydratedAssignment dehyAssign = hydrater.dehydrate(assignInput);
        log.info("After dehydrate registry: \n" + registry.toBigString());
        log.info("Dehydrated: " + dehyAssign);
        AssignmentInstance assignOutput = hydrater.hydrate(dehyAssign);
        assertEquals(assignInput, assignOutput);
    }

    @Test
    public void shouldRoundTripFlat() throws Exception {
        AssignmentInstance assignInput = AssignmentInstance.make(
                "NAME", "STEVE",
                "ADDR", "2301 Forest Hill",
                "SEX", "M"
        );
        AssignmentInstance assignInput2 = AssignmentInstance.make(
                "NAME", "BOB",
                "ADDR", "2301 Forest Hill",
                "SEX", "F"
        );
        DehydratedAssignment dehydrate = hydrater.dehydrate(assignInput);
        log.info("Dehydrated " + dehydrate);
        DehydratedAssignment dehydrate2 = hydrater.dehydrate(assignInput2);
        log.info("Dehydrated2 " + dehydrate2);

        AssignmentInstance rehyd1 = hydrater.hydrate(dehydrate);
        AssignmentInstance rehyd2 = hydrater.hydrate(dehydrate2);

        assertEquals(assignInput, rehyd1);
        assertEquals(assignInput2, rehyd2);
    }
}