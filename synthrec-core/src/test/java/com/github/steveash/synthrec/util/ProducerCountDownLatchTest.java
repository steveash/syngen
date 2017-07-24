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

package com.github.steveash.synthrec.util;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * @author Steve Ash
 */
public class ProducerCountDownLatchTest {

    @Rule
    public Timeout timeout = new Timeout(15, TimeUnit.SECONDS);

    @Test
    public void shouldSimpleSingleThreaded() throws Exception {
        ProducerCountDownLatch latch = new ProducerCountDownLatch();
        assertFalse(latch.latchClosed());
        latch.produceOne();
        latch.produceOne();
        latch.produceOne();

        latch.consumeOne();
        latch.producerComplete();
        latch.consumeOne();

        assertFalse(latch.await(10, TimeUnit.MILLISECONDS));

        assertFalse(latch.latchClosed());
        latch.consumeOne();
        assertTrue(latch.latchClosed());
        // if this fails it will timeout
        latch.await();
    }

    @Test
    public void shouldFailGracefully() throws Exception {
        ProducerCountDownLatch latch = new ProducerCountDownLatch();
        Throwable t = new Throwable("my failure");
        UncheckedExecutionException got = null;

        assertFalse(latch.latchClosed());
        latch.produceOne();
        latch.produceOne();
        latch.failure(t);

        assertFalse(latch.latchClosed());
        latch.consumeOne();
        assertFalse(latch.await(10, TimeUnit.MILLISECONDS));
        latch.consumeOne();
        try {
            latch.await(10, TimeUnit.MILLISECONDS);
            fail("shouldve failed");
        } catch (UncheckedExecutionException e) {
            got = e;
        }

        assertTrue(latch.latchClosed());

        try {
            latch.await();
            fail("shouldve kept throwing");
        } catch (UncheckedExecutionException e) {
            // good
        }
        assertTrue(t == got.getCause());
    }
}