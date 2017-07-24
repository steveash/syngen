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

package com.github.steveash.synthrec.string;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Ash
 */
public class PatternExpanderTest {
    private static final Logger log = LoggerFactory.getLogger(PatternExpanderTest.class);

    private static final String[] PATS = new String[]{"9999AAAA",
            "9/9",
            "AAAAAAAAA99A",
            "999A99",
            "9A99999",
            "A999999",
            "AAAAAAAAA9A",
            "AA900",
            "A/9",
            "999/9",
            "$999",
            "A999AA",
            "9A99A",
            "9999AAAAAAA",
            "AAA900",
            "AAAAA9999",
            "@99A",
            "@9A",
            "999)",
            "99AAAAAAAA",
            "AA9999A",
            "A9999A"
    };

    private RandomGenerator rand;

    @Before
    public void setup() {rand = new Well19937c(0xCAFE123);}

    @Test
    public void shouldSampleWithDict() throws Exception {
        PatternExpander pe = new PatternExpander(2, BinnedDictSamplerTest.makeSampleBinnedDict());
        for (String pat : PATS) {
            String hydrated = pe.expand(rand, pat);
            log.info("expanded " + pat + " to " + hydrated);
            assertTrue("expanded " + pat + " to " + hydrated, Math.abs(hydrated.length() - pat.length()) < 10.0);
        }
    }

    @Test
    public void shouldSampleWithoutDict() throws Exception {
        PatternExpander pe = new PatternExpander();
        for (String pat : PATS) {
            String hydrated = pe.expand(rand, pat);
            log.info("expanded " + pat + " to " + hydrated);
            assertEquals(hydrated.length(), pat.length());
        }
    }

    @Test
    public void shouldNotThrowOobException() throws Exception {
        PatternExpander pe = new PatternExpander();
        RandomGenerator rand = mock(RandomGenerator.class);
                when(rand.nextInt(anyInt())).thenReturn(3);
        assertThat(pe.expand(rand, "{")).isEqualTo("{");
        assertThat(pe.expand(rand, "}")).isEqualTo("}");
        assertThat(pe.expand(rand, "9{")).isEqualTo("4{"); // 4 because we sample [0-8] then add 1
        assertThat(pe.expand(rand, "9{{")).isEqualTo("4{{");
        assertThat(pe.expand(rand, "9{{A}}")).isEqualTo("4A");
    }

    @Test
    public void shouldLeaveEscaped() throws Exception {
        RandomGenerator rand = mock(RandomGenerator.class);
        when(rand.nextInt(anyInt())).thenReturn(3);
        PatternExpander exp = new PatternExpander(Integer.MAX_VALUE, null);
        assertEquals("433-999-4", exp.expand(rand, "999-{{999}}-9"));
        assertEquals("433-99{{9}}-4", exp.expand(rand, "999-{{99{{9}}}}-9"));
        assertEquals("433-99{{9}}-3ABC", exp.expand(rand, "999-{{99{{9}}}}-9{{ABC}}"));
        assertEquals("A333-99{{9}}-3ABC", exp.expand(rand, "{{A}}999-{{99{{9}}}}-9{{ABC}}"));
    }
}