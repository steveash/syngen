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

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

import com.google.common.base.Preconditions;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * @author Steve Ash
 */
public class DirichletSampler<T> {

    /**
     * Creates a sampler from a categorical distribution (i.e. density that sums to one) using a virtual
     * observation count + a hyper alpha (add-k smoothing)
     * @param categorical
     * @param virtualObservations
     * @param hyperAlpha
     * @param <T>
     * @return
     */
    public static <T> DirichletSampler<T> fromCategoricalAndHyperPrior(Multinomial<T> categorical,
            long virtualObservations,
            double hyperAlpha
    ) {
        Preconditions.checkArgument(categorical.isNormalized(), "gotta pass normalized categorical");
        MutableMultinomial<T> dist = new MutableMultinomial<>(categorical.maxEntries());
        for (Entry<T> entry : categorical.entries()) {
            dist.add(entry.getKey(), (entry.getDoubleValue() * ((double) virtualObservations)) + hyperAlpha);
        }
        return new DirichletSampler<>(dist);
    }

    public static <T> DirichletSampler<T> fromMultinomialAndHyperPrior(Multinomial<T> multinomial, double hyperAlpha) {
        Preconditions.checkArgument(multinomial.sum() > 1.0, "gotta pass a multinomial");
        MutableMultinomial<T> dist = new MutableMultinomial<>(multinomial.maxEntries());
        for (Entry<T> entry : multinomial.entries()) {
            dist.add(checkNotNull(entry.getKey()), entry.getDoubleValue() + hyperAlpha);
        }
        Preconditions.checkArgument(dist.size() > 0);
        return new DirichletSampler<>(dist);
    }

    public static <T> DirichletSampler<T> fromMultinomialAndPrior(Multinomial<T> empirical,
            Multinomial<T> prior, double hyperAlpha) {
        return fromMultinomialAndPrior(empirical, new MultinomialPriorDist<T>(prior), hyperAlpha);
    }

    public static <T> DirichletSampler<T> fromMultinomialAndPrior(Multinomial<T> empirical,
            PriorDist<T> prior, double hyperAlpha) {

        Preconditions.checkArgument(empirical.sum() > 1.0, "gotta pass a multinomial");
        MutableMultinomial<T> dist = MutableMultinomial.createUnknownMax();
        // adding prior first because prior doesn't guarentee keys are unique -- dont want to double count so just "setting"
        for (T key : prior.allKeys()) {
            double count = prior.countFor(key);
            if (count > 0) {
                dist.set(key, count);
            }
        }
        // now add all empirical
        dist.addMultinomial(empirical);

        if (hyperAlpha > 0) {
            ObjectIterator<Entry<T>> iter = dist.entries().fastIterator();
            while (iter.hasNext()) {
                Entry<T> entry = iter.next();
                dist.add(checkNotNull(entry.getKey()), hyperAlpha);
            }
        }
        return new DirichletSampler<>(dist);
    }

    private final Multinomial<T> dirichletMulti;
    private final RandomGenerator rng;

    private DirichletSampler(Multinomial<T> dirichletMulti) {
        this.dirichletMulti = dirichletMulti;
        this.rng = RandUtil.threadLocalRand();
    }

    public Multinomial<T> sampleNormalized() {
        MutableMultinomial<T> multi = sampleMultinomial();
        return multi.normalize();
    }

    public MutableMultinomial<T> sampleMultinomial() {
        MutableMultinomial<T> multi = new MutableMultinomial<>(dirichletMulti.maxEntries());
        for (Entry<T> entry : dirichletMulti.entries()) {
            GammaDistribution dist = new GammaDistribution(rng, entry.getDoubleValue(), 1.0);
            double sample = dist.sample();
            multi.add(entry.getKey(), sample);
        }
        return multi;
    }

    public SamplingTable<T> sampleTable() {
        MutableMultinomial<T> multinomial = sampleMultinomial();
        return SamplingTable.createFromMultinomial(multinomial);
    }
}
