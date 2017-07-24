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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * A count down latch for a single producer and multiple consumers
 * producer calls produceOne() for each produced elements
 * consumers call consumeOne() for each element consumed
 * producer calls producerComplete() after its all done producing things (since theres only one producer, no race)
 * anyone waiting for everything to be done, can await() on the latch and get unblocked when all production/consumption
 *  is complete
 *
 *  Orderings:
 *  a) P: done = true, P: outstanding > 0, C: outstanding == 0, C: done = true
 * @author Steve Ash
 */
public class ProducerCountDownLatch {

    public static class ProducerLatchFailureException extends RuntimeException {
        private static final long serialVersionUID = -6523393779856981322L;

        public ProducerLatchFailureException() {
        }

        public ProducerLatchFailureException(String message) {
            super(message);
        }

        public ProducerLatchFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final ProducerLatchFailureException ex = new ProducerLatchFailureException();

    private final AtomicInteger outstanding = new AtomicInteger();
    private volatile boolean producedDone = false;
    private final CountDownLatch doneLatch = new CountDownLatch(1);
    private volatile Throwable failure = null;

    public int outstandingRequests() {
        return outstanding.get();
    }

    public boolean latchClosed() {
        return producedDone && outstanding.get() <= 0;
    }

    public void produceOne() {
        if (!producedDone) {
            outstanding.incrementAndGet();
        }
    }

    public void failure(Throwable t) {
        if (t instanceof ProducerLatchFailureException) {
            // no op
        }
        if (failure == null) {
            failure = t;
            producerComplete();
        } else {
            failure.addSuppressed(t);
        }
    }

    public void producerComplete() {
        // producedDone -> outstanding are our two synchronizers; these are both volatile so they wont
        // be reordered. This or the "last" consumer needs to notify the waiters
        // we first set the flag and then check the outstanding count. We know that there will be
        // nothing else produced so we the outstanding can't go up. We either observe zero first or second+
        // but in any case we can countdown (extra countdowns dont hurt)
        // but by setting the producedDone first we guarentee that either us or the racing "last" one will
        // do the countdown
        producedDone = true;
        int current = outstanding.get();
        if (current == 0) {
            doneLatch.countDown();
        }
    }

    public void consumeOne() {
        int current = outstanding.decrementAndGet();
        boolean currentDone = producedDone;
        // get the currentDone _after_ the decrement
        if (currentDone && current == 0) {
            doneLatch.countDown();
        }
    }

    public void await() throws InterruptedException {
        doneLatch.await();
        if (failure != null) {
            throw new UncheckedExecutionException(failure);
        }
    }

    public boolean await(long time, TimeUnit unit) throws InterruptedException {
        boolean completed = doneLatch.await(time, unit);
        if (completed) {
            if (failure != null) {
                throw new UncheckedExecutionException(failure);
            }
        }
        return completed;
    }
}
