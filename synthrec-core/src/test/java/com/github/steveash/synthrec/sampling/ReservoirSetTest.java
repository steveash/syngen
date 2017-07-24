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

package com.github.steveash.synthrec.sampling;

import static org.junit.Assert.*;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Steve Ash
 */
public class ReservoirSetTest {

    @Test
    public void shouldSample() throws Exception {
        RandomGenerator mock = Mockito.mock(RandomGenerator.class);
        when(mock.nextInt(anyInt())).thenReturn(4, 0, 6, 1, 4, 4, 0);
        ReservoirSet<String> set = new ReservoirSet<>(3);
        set.tryAdd(mock, "A");
        set.tryAdd(mock, "B");
        set.tryAdd(mock, "C");
        set.tryAdd(mock, "D"); // 4: skip this
        set.tryAdd(mock, "E"); // 5: del A ins E
        set.tryAdd(mock, "F"); // 6: skip
        set.tryAdd(mock, "B"); // 6: noop
        set.tryAdd(mock, "C"); // 6: noop
        set.tryAdd(mock, "G"); // 7: del B ins G
        set.tryAdd(mock, "H"); // 8: skip
        set.tryAdd(mock, "I"); // 9: skip
        set.tryAdd(mock, "J"); // 10: del E ins J

        assertThat(set.getFinalSet()).containsAllOf("J", "G", "C");
    }
}