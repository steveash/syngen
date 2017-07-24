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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

/**
 * An identity map that can be inserted into a stream pipeline to report progress
 * @author Steve Ash
 */
public class StreamCounter<T> implements Function<T,T> {
    private static final Logger log = LoggerFactory.getLogger(StreamCounter.class);

    private final RateLimiter limiter = RateLimiter.create(0.20, 5, TimeUnit.SECONDS);
    private final AtomicLong counter = new AtomicLong();
    private final String label;

    public StreamCounter(String label) {this.label = label;}

    @Override
    public T apply(T t) {
        long newCount = counter.incrementAndGet();
        if (newCount % 128 == 0 && limiter.tryAcquire()) {
            log.info("Completed " + newCount + "...");
        }
        return t;
    }
}
