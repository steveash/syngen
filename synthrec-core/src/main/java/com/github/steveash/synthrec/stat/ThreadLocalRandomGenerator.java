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

package com.github.steveash.synthrec.stat;

import java.util.function.Supplier;

import org.apache.commons.math3.random.RandomGenerator;

/**
 * Thread confined random generator
 * @author Steve Ash
 */
public class ThreadLocalRandomGenerator implements RandomGenerator {

    private final ThreadLocal<RandomGenerator> localGens;

    public ThreadLocalRandomGenerator(Supplier<RandomGenerator> randSupplier) {
        this.localGens = ThreadLocal.withInitial(randSupplier);
    }

    public RandomGenerator getThreadGenerator() {
        return localGens.get();
    }

    @Override
    public void setSeed(int seed) {
        localGens.get().setSeed(seed);
    }

    @Override
    public void setSeed(int[] seed) {
        localGens.get().setSeed(seed);
    }

    @Override
    public void setSeed(long seed) {
        localGens.get().setSeed(seed);
    }

    @Override
    public void nextBytes(byte[] bytes) {
        localGens.get().nextBytes(bytes);
    }

    @Override
    public int nextInt() {
        return localGens.get().nextInt();
    }

    @Override
    public int nextInt(int n) {
        return localGens.get().nextInt(n);
    }

    @Override
    public long nextLong() {
        return localGens.get().nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return localGens.get().nextBoolean();
    }

    @Override
    public float nextFloat() {
        return localGens.get().nextFloat();
    }

    @Override
    public double nextDouble() {
        return localGens.get().nextDouble();
    }

    @Override
    public double nextGaussian() {
        return localGens.get().nextGaussian();
    }
}
