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

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.string.PatternExpander;
import com.github.steveash.synthrec.string.PatternReducer;

/**
 * Decorator for a sampler that intercepts sampled PATTERNs and generates a random sample of the
 * pattern
 *
 * You can optionally pass a binnedDictSampler if you want to use real words when you replace patterns
 * that have A's >= 3 in length
 * @see PatternReducer
 * @see com.github.steveash.synthrec.string.StringBinner
 * @see PatternExpander
 * @author Steve Ash
 */
public class PatternSamplerDecorator implements Sampler<String> {

    private final Sampler<?> delegate;
    private final PatternExpander expander;

    public PatternSamplerDecorator(Sampler<?> delegate, PatternExpander expander) {
        this.delegate = delegate;
        this.expander = expander;
    }

    @Override
    public String sample(RandomGenerator rand) {
        String sample = (String) delegate.sample(rand);
        return expander.expandIfNeeded(rand, sample);
    }
}
