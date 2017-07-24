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
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.github.steveash.jg2p.util.Percent;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.math.DoubleMath;

import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.AbstractObject2DoubleMap.BasicEntry;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.FastEntrySet;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Discrete categorical or multinomial distribution; you can sum things into the multinomial
 * and use it that way -- or you can normalize() it to a categorical distribution (i.e. a probability
 * distribution over cases)
 * @author Steve Ash
 */
public class Multinomial<T> implements Serializable {

    private static final long serialVersionUID = 9024316754729686564L;

    private static final Multinomial EMPTY = new Multinomial(-1);

    public static <T> Multinomial<T> empty() {
        return EMPTY;
    }

    public static <T> MutableMultinomial<T> makeNormalizedFrom(T... elements) {
        MutableMultinomial<T> dens = makeFrom(elements);
        return dens.normalize();
    }

    public static <T> MutableMultinomial<T> makeFrom(T... elements) {
        HashMultiset<T> multiset = HashMultiset.create(Arrays.asList(elements));
        MutableMultinomial<T> dens = new MutableMultinomial<>(multiset.entrySet().size());
        for (Multiset.Entry<T> entry : multiset.entrySet()) {
            dens.add(entry.getElement(), entry.getCount());
        }
        return dens;
    }

    public interface EntryFunction<T, R> {
        R apply(T element, double value);
    }

    private static final int LINELEN = 119;
    protected final Object2DoubleOpenHashMap<T> distrib;
    protected int maxEntries;

    // max entries can be negative in the mutable version if you dont know before how how many there are
    // so everything needs to be safe for that and for things that need it (like entropy) then just use NaNs?
    public Multinomial(int maxEntries) {
        this.maxEntries = maxEntries;
        distrib = new Object2DoubleOpenHashMap<>(maxEntries > 0 ? maxEntries : Object2DoubleOpenHashMap.DEFAULT_INITIAL_SIZE);
    }

    protected Multinomial(int maxEntries, int initialCapacity) {
        this.maxEntries = maxEntries;
        distrib = new Object2DoubleOpenHashMap<>(initialCapacity);
    }

    protected Multinomial(int maxEntries, Object2DoubleOpenHashMap<T> values) {
        this.maxEntries = maxEntries;
        this.distrib = values;
        if (maxEntries > 0) {
            Preconditions.checkState(values.size() <= this.maxEntries,
                    "table exceeded maxEntries",
                    distrib.size(),
                    this.maxEntries
            );
        }
    }

    public Set<T> keySet() {
        return distrib.keySet();
    }

    public double get(Object key) {
        return distrib.getDouble(key);
    }

    public boolean contains(Object key) {
        return distrib.containsKey(key);
    }

    public double entropy() {
        DoubleIterator iterator = distrib.values().iterator();
        double sum = 0;
        double sump = 0;
        while (iterator.hasNext()) {
            double p = iterator.nextDouble();
            if (p > 0) {
                sum += (p * DoubleMath.log2(p));
                sump += p;
            }
        }
        Preconditions.checkState(DoubleMath.fuzzyEquals(1.0, sump, 0.001),
                "cant call entropy on unnormalized dist",
                sump
        );
        if (sum < 0) {
            return -sum;
        }
        return sum;
    }

    /**
     * Calculates the diversity metric; q = 1 is the geometric mean, q = 2 is the arithmetic mean.
     * Higher values of q give more weight to abundent classes; values closer to zero give weight to exotic
     * cases; probably not sensicle to pass q values < 0
     * @param q
     * @return
     */
    public double diversity(double q) {
        Preconditions.checkArgument(q != 1.0, "diversity isnt defined at 1.0; see entropy");
        DoubleIterator iterator = distrib.values().iterator();
        double sum = 0;
        double sump = 0;
        while (iterator.hasNext()) {
            double p = iterator.nextDouble();
            if (p > 0) {
                sum += Math.pow(p, q);
                sump += p;
            }
        }
        Preconditions.checkState(DoubleMath.fuzzyEquals(1.0, sump, 0.001),
                "cant call diversity on unnormalized dist",
                sump
        );
        return Math.pow(sum, (1.0 / (1.0 - q)));
    }

    /**
     * Return the sum of this multinomial (will return 1 if this is normalized/categorical)
     * @return
     */
    public double sum() {
        double sum = 0;
        DoubleIterator iterator = distrib.values().iterator();
        while (iterator.hasNext()) {
            sum += iterator.nextDouble();
        }
        return sum;
    }

    @Nullable
    public T sample(RandomGenerator rand) {
        if (isEmpty()) {
            throw new IllegalStateException("cannot sample from empty multinomial");
        }
        double sample = rand.nextDouble() * sum(); // might not be normalized so this way we can use the "cdf"
        double cdf = 0.0;
        T lastEntry = null;
        for (Entry<T> entry : distrib.object2DoubleEntrySet()) {
            cdf += entry.getDoubleValue();
            lastEntry = entry.getKey();
            if (cdf >= sample) {
                break;
            }
        }
        return checkNotNull(lastEntry, "was about to return a null sample", this);
    }

    public int maxEntries() {
        return maxEntries;
    }

    public double entropyForNonNormalized() {
        DoubleIterator iterator0 = distrib.values().iterator();
        double denom = 0.0;
        while (iterator0.hasNext()) {
            denom += iterator0.nextDouble();
        }
        DoubleIterator iterator = distrib.values().iterator();
        double sum = 0;
        while (iterator.hasNext()) {
            double p = iterator.nextDouble();
            if (p > 0) {
                double prob = p / denom;
                sum += (prob * DoubleMath.log2(prob));
            }
        }
        if (sum < 0) {
            return -sum;
        }
        return sum;
    }

    public boolean isNormalized() {
        DoubleIterator iter = distrib.values().iterator();
        double sum = 0;
        while (iter.hasNext()) {
            sum += iter.nextDouble();
        }
        return DoubleMath.fuzzyEquals(sum, 1.0, 0.00001);
    }

    public double entropyPercOfMax() {
        return Percent.value(entropy(), maxEntropy());
    }

    public T best() {
        T result = null;
        double best = 0;
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() > best) {
                best = entry.getDoubleValue();
                result = entry.getKey();
            }
        }
        return result;
    }

    public double bestProbability() {
        double best = 0;
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() > best) {
                best = entry.getDoubleValue();
            }
        }
        return best;
    }

    public double maxEntropy() {
        if (maxEntries > 0) {
            return DoubleMath.log2(maxEntries);
        } else {
            return DoubleMath.log2(this.size());
        }
    }

    /**
     * Get a ranked list of entries from highest value to lowest (descending order by value)
     * @return
     */
    public List<Object2DoubleMap.Entry<T>> rankedList() {
        ArrayList<Object2DoubleMap.Entry<T>> objects = Lists.newArrayListWithCapacity(distrib.size());
        distrib.object2DoubleEntrySet().forEach(e -> objects.add(new BasicEntry<T>(e.getKey(), e.getDoubleValue())));
        objects.sort(Comparator.<Object2DoubleMap.Entry<T>>comparingDouble(Entry::getDoubleValue).reversed());
        return objects;
    }

    /**
     * @return all the entries in this -- REMEMBER fast entry set reuses the same entry! dont let it escape
     */
    public FastEntrySet<T> entries() {
        return distrib.object2DoubleEntrySet();
    }

    public int size() {
        return distrib.size();
    }

    public boolean isEmpty() {
        return distrib.isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public int countAbove(double greaterThanOrEqualToThresh) {
        return (int) distrib.object2DoubleEntrySet().stream()
                .filter(e -> e.getDoubleValue() >= greaterThanOrEqualToThresh)
                .count();
    }

    public double minValue() {
        double minCount = Double.POSITIVE_INFINITY;
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() > 0) {
                if (entry.getDoubleValue() < minCount) {
                    minCount = entry.getDoubleValue();
                }
            }
        }
        return minCount < Double.POSITIVE_INFINITY ? minCount : 0;
    }

    public double maxValue() {
        double maxValue = Double.NEGATIVE_INFINITY;
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() > 0) {
                if (entry.getDoubleValue() > maxValue) {
                    maxValue = entry.getDoubleValue();
                }
            }
        }
        return maxValue > Double.NEGATIVE_INFINITY ? maxValue : 0;
    }

    public int countWithMinValue() {
        double minValue = minValue();
        int count = countWithValueLte(minValue);
        return count;
    }

    public int countWithValueLte(double minValue) {
        int count = 0;
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() > 0) {
                if (entry.getDoubleValue() <= minValue) {
                    count += 1;
                }
            }
        }
        return count;
    }

    public int countNonZero() {
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        int count = 0;
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() > 0) {
                count += 1;
            }
        }
        return count;
    }

    public List<Object2DoubleMap.Entry<T>> entriesAboveThresh(double greaterThanOrEqualToThresh) {
        ArrayList<Object2DoubleMap.Entry<T>> objects = Lists.newArrayList();
        distrib.object2DoubleEntrySet().stream()
                .filter(e -> e.getDoubleValue() >= greaterThanOrEqualToThresh)
                .forEach(e -> objects.add(new BasicEntry<T>(e.getKey(), e.getDoubleValue())));
        return objects;
    }

    public Multinomial<T> threshold(double onlyAllowGreaterThanOrEqual) {
        MutableMultinomial<T> copy = new MutableMultinomial<>(this.maxEntries);
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<T> entry = iter.next();
            if (entry.getDoubleValue() >= onlyAllowGreaterThanOrEqual) {
                copy.add(entry.getKey(), entry.getDoubleValue());
            }
        }
        return copy.normalize().toImmutable();
    }

    public <R> Iterable<R> nonZeroEntries(EntryFunction<T, R> func) {
        return transform(filter(distrib.object2DoubleEntrySet(), e -> e.getDoubleValue() > 0),
                entry -> func.apply(entry.getKey(), entry.getDoubleValue())
        );
    }

    public DescriptiveStatistics statsOverCounts() {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            stats.addValue(iter.next().getDoubleValue());
        }
        return stats;
    }

    public SummaryStatistics summaryStatsOverCounts() {
        SummaryStatistics stats = new SummaryStatistics();
        ObjectIterator<Entry<T>> iter = distrib.object2DoubleEntrySet().fastIterator();
        while (iter.hasNext()) {
            stats.addValue(iter.next().getDoubleValue());
        }
        return stats;
    }

    public double jensonShannonDivergence(Multinomial<T> other) {
        Preconditions.checkArgument(other.isNormalized(),
                "cant get metric distnace on multinomial must be a prop dist"
        );
        Preconditions.checkArgument(this.isNormalized(), "cant get metric distnace on multinomial must be a prop dist");
        Multinomial<T> average = average(this, other);
        double kl1 = this.kullbackLieblerTo(average);
        double kl2 = other.kullbackLieblerTo(average);
        double js = (kl1 + kl2) / 2.0;
        return js;
    }

    /**
     * returns KL divergence KL( this || q ) (i.e. this density it the "true" density)
     * If any entry in Q is zero then this returns POSITIVE_INFINITY
     * @param q
     * @return
     */
    public double kullbackLieblerTo(Multinomial<T> q) {
        SetView<T> keys = Sets.union(this.distrib.keySet(), q.distrib.keySet());
        int numKeysRemaining = this.distrib.size();
        double result = 0.0;
        double assignedMass1 = 0.0;
        double assignedMass2 = 0.0;
        double log2 = Math.log(2.0);
        double p1, p2;
        double epsilon = 1e-10;

        for (T key : keys) {
            p1 = this.distrib.getDouble(key);
            p2 = q.distrib.getDouble(key);
            numKeysRemaining--;
            assignedMass1 += p1;
            assignedMass2 += p2;
            if (p1 < epsilon) {
                continue;
            }
            double logFract = Math.log(p1 / p2);
            if (logFract == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY; // can't recover
            }
            result += p1 * (logFract / log2); // express it in log base 2
        }

        if (numKeysRemaining != 0) {
            p1 = (1.0 - assignedMass1) / numKeysRemaining;
            if (p1 > epsilon) {
                p2 = (1.0 - assignedMass2) / numKeysRemaining;
                double logFract = Math.log(p1 / p2);
                if (logFract == Double.POSITIVE_INFINITY) {
                    return Double.POSITIVE_INFINITY; // can't recover
                }
                result += numKeysRemaining * p1 * (logFract / log2); // express it in log base 2
            }
        }
        return result;
    }

    public MutableMultinomial<T> normalizedCopy() {
        return MutableMultinomial.copyFrom(this).normalize();
    }

    public MutableMultinomial<T> copy() {
        return MutableMultinomial.copyFrom(this);
    }

    public void fillDensity(Comparator<T> ordering, double[] sink) {
        Preconditions.checkArgument(sink.length == maxEntries, "must pass a sink of the same length");
        Preconditions.checkArgument(distrib.size() == maxEntries, "can only fill with a fully specified distribution");
        List<T> sorted = Ordering.from(ordering).sortedCopy(distrib.keySet());
        for (int i = 0; i < sorted.size(); i++) {
            sink[i] = distrib.getDouble(sorted.get(i));
        }
    }

    public static <K> Multinomial<K> weightedAverage(Multinomial<K> d1, double w1, Multinomial<K> d2) {
        Preconditions.checkArgument(d1.maxEntries == d2.maxEntries, "must have same max entries");
        double w2 = 1.0 - w1;
        SetView<K> keys = Sets.union(d1.distrib.keySet(), d2.distrib.keySet());
        MutableMultinomial<K> averaged = new MutableMultinomial<>(d1.maxEntries);

        for (K key : keys) {
            double newProbability = d1.get(key) * w1 + d2.get(key) * w2;
            averaged.set(key, newProbability);
        }
        return averaged.normalize().toImmutable();
    }

    public static <K> Multinomial<K> average(Multinomial<K> d1, Multinomial<K> d2) {
        return weightedAverage(d1, 0.5, d2);
    }

    public void printTo(PrintWriter pw) {
        printTo(pw, a -> a);
    }

    public void printTo(PrintWriter pw, Function<T, Object> xform) {
        printTo(this, pw, xform);
    }

    @Override
    public String toString() {
        StringWriter writer = new StringWriter();
        printTo(this, new PrintWriter(writer));
        return writer.toString();
    }

    public static void printTo(Multinomial<?> density, PrintWriter pw) {
        printTo(density, pw, a -> a);
    }

    public static <T> void printTo(Multinomial<T> density, PrintWriter pw, Function<T, Object> xform) {
        List<? extends Entry<T>> entries = density.rankedList();
        DoubleSummaryStatistics stats = entries.stream().collect(Collectors.summarizingDouble(Entry::getDoubleValue));
        boolean isNormalized = DoubleMath.fuzzyEquals(1.0, stats.getSum(), 0.0001);
        pw.println(StringUtils.center(String.format(" Min=%.4f  Max=%.4f  Entropy=%.3f (%.1f%%) %s ",
                stats.getMin(),
                stats.getMax(),
                (isNormalized ? density.entropy() : density.entropyForNonNormalized()),
                (isNormalized && density.maxEntries > 0 ? density.entropyPercOfMax() * 100.0 : 0),
                (isNormalized ? "N" : "U")
        ), LINELEN, '*'));
        Optional<Integer> maxElem = entries.stream()
                .map(e -> xform.apply(e.getKey()).toString().length())
                .max(Comparator.naturalOrder());
        int maxVal = 6;
        if (!isNormalized) {
            Optional<Integer> maxValLen = entries.stream()
                    .map(e -> fmt(e.getDoubleValue()).length())
                    .max(Comparator.naturalOrder());
            maxVal = maxValLen.orElse(maxVal);
        }
        int pad = maxElem.orElse(10) + maxVal + 5;
        int grid = Math.max(0, LINELEN - pad);
        double minVal = 0.0;
        double spreadVal = 1.0;
        if (!isNormalized) {
            minVal = 0; //stats.getMin();
            spreadVal = stats.getMax() - minVal;
        }
        for (Entry<T> entry : entries) {
            double percVal = entry.getDoubleValue();
            String valFormat;
            if (isNormalized) {
                valFormat = String.format("%.4f", percVal);
            } else {
                percVal = (percVal - minVal) / spreadVal;
                valFormat = StringUtils.leftPad(fmt(entry.getDoubleValue()), maxVal, ' ');
            }
            int on = Math.min(grid, (int) (percVal * ((double) grid)));
            int off = grid - on;

            String format = String.format("%s (%s) ", xform.apply(entry.getKey()).toString(), valFormat);
            pw.println(
                    StringUtils.leftPad(format, pad) +
                            StringUtils.repeat('#', on) +
                            StringUtils.repeat('-', off)
            );
        }
        pw.println(StringUtils.repeat('*', LINELEN));
    }

    private static String fmt(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            return String.format("%.1f", d);
        }
    }
}
