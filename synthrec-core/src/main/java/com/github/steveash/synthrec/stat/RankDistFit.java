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

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolverUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.github.steveash.synthrec.util.BestBox;
import com.github.steveash.synthrec.util.DoublePair;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * A calculator for ranked lists (like top-k lists) which have some kind of curvature that might not really
 * fit a nice power distribution or a zipfian distribution -- but its along those lines.  This contains a number
 * of different least squares fit for different piecewise distributions:
 * - linear (single OLS linear regression)
 * - linear with a "tail" (a piecewise split using a single linear up to a split point then just a horizontal line)
 * - two linear (piecewise split with two linear regressions)
 * - single power distribution ( a * n**b )
 * - single power distrib with a "tail" (power then horizontal line)
 * - two power distrib (this works best for most)
 * @author Steve Ash
 */
public class RankDistFit {
    private static final Logger log = LoggerFactory.getLogger(RankDistFit.class);

    public static class RankPoint {
        private final int rank;     // rank 0 is the max value
        private final double value; // the value of the element at this rank

        public RankPoint(int rank, double value) {
            this.rank = rank;
            this.value = value;
        }

        public int getRank() {
            return rank;
        }

        public double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "RankPoint{" +
                    "rank=" + rank +
                    ", value=" + value +
                    '}';
        }
    }

    /**
     * Create a RankDistFitter from the given multinomial distribution
     * @param multi
     * @return
     */
    public static RankDistFit fromMultinomial(Multinomial<?> multi) {
        double[] pts = new double[multi.countNonZero()];
        ObjectIterator<? extends Entry<?>> iter = multi.entries().fastIterator();
        int i = 0;
        while (iter.hasNext()) {
            Entry<?> entry = iter.next();
            if (entry.getDoubleValue() > 0) {
                pts[i] = entry.getDoubleValue();
                i += 1;
            }
        }
        Preconditions.checkState(pts.length == i, "counted and inserted different numbers");
        Arrays.sort(pts);
        ArrayUtils.reverse(pts);
        return new RankDistFit(pts);
    }

    /**
     * Make a rank dist fitter from an array list of points; they dont have to be sorted, but if they
     * are reverse sorted then it saves the sorting work
     * @param points
     * @return
     */
    public static RankDistFit fromDoubleArray(DoubleArrayList points) {
        double[] mypts = points.toDoubleArray();
        if (!isReverseSorted(points)) {
            Arrays.sort(mypts);
            ArrayUtils.reverse(mypts);
        }
        return new RankDistFit(mypts);
    }

    private static boolean isReverseSorted(DoubleArrayList points) {
        if (points.isEmpty()) return true;
        double last = points.getDouble(0);
        for (int i = 1; i < points.size(); i++) {
            double next = points.getDouble(i);
            if (next > last) {
                return false;
            }
            last = next;
        }
        return true;
    }

    private final double[] points; // sorted desc so points[0] is the max

    private RankDistFit(double[] points) {this.points = points;}

    public BestBox<FitDist> fitBest() {
        BestBox<FitDist> minimizer = BestBox.minimizer();
        ToDoubleFunction<FitDist> scorer = (fd) -> sse(fd, this.points);
        minimizer.tryUpdate(fitLinear(), scorer);
        minimizer.tryUpdate(fitLinearPlusTail(), scorer);
        minimizer.tryUpdate(fitTwoLinear(), scorer);
        minimizer.tryUpdate(fitPowerDist(), scorer);
        minimizer.tryUpdate(fitPowerWithTailDist(), scorer);
        minimizer.tryUpdate(fitTwoPowerDist(), scorer);
        return minimizer;
    }

    /**
     * Fits a single linear model over the whole thing
     * @return
     */
    public FitDist fitLinear() {
        SimpleRegression regression = linearRegressionOnSubset(0, this.points.length);
        return new TwoLinearDist(0, 0, 0, regression.getSlope(), regression.getIntercept());
    }

    /**
     * Fits a single linear model over the first part, then just fits a single line of the second part
     * @return
     */
    public TwoLinearDist fitLinearPlusTail() {
        IntFunction<TwoLinearDist> maker = this::makeLinearPlusTailAt;
        return fitDistWithSplitPoint(maker);
    }

    /**
     * Fits a single linear model of the first part and another linear model over the second part; this
     * tries to find the best place to split the ranked list based on minimizing SSE
     * @return
     */
    public TwoLinearDist fitTwoLinear() {
        IntFunction<TwoLinearDist> maker = this::makeTwoLinearAt;
        return fitDistWithSplitPoint(maker);
    }

    /**
     * Fits a single power distribution over the whole ranked list
     * @return
     */
    public PowerDist fitPowerDist() {
        final Iterable<DoublePair> inputXy = DoublePair.ranked(new DoubleArrayList(this.points));
        return fitPowerToIter(inputXy);
    }

    /**
     * Fits a single power distribution over the first part and a single horizontal line over the second part
     * @return
     */
    public PowerDist fitPowerWithTailDist() {
        return this.fitDistWithSplitPoint(this::makePowerTailAt);
    }

    /**
     * Fits a power distribution over the first part and a separate power distribution over the second
     * part, finding a split point that minimizes SSE
     * @return
     */
    public TwoPowerDist fitTwoPowerDist() {
        return this.fitDistWithSplitPoint(this::makeTwoPowerAt);
    }

    /**
     * Calculates the sum of the squared error for the given points on this model
     * @param dist
     * @param points
     * @return
     */
    public static double sse(FitDist dist, double[] points) {
        double sse = 0;
        for (int i = 0; i < points.length; i++) {
            double error = dist.predict(i) - points[i];
            sse += (error * error);
        }
        return sse;
    }

    public static double sse(FitDist dist, DoubleArrayList points) {
        double sse = 0;
        for (int i = 0; i < points.size(); i++) {
            double error = dist.predict(i) - points.getDouble(i);
            sse += (error * error);
        }
        return sse;
    }

    private <T extends FitDist> T fitDistWithSplitPoint(IntFunction<T> distMaker) {
        log.debug("Fitting {} points", points.length);
        Preconditions.checkState(points.length > 0, "cant fit to zero points");
        BestBox<T> minimizer = BestBox.minimizer();
        ToDoubleFunction<T> scorer = d -> sse(d, points);
        minimizer.tryUpdate(distMaker.apply(0), scorer);
        minimizer.tryUpdate(distMaker.apply(points.length), scorer);

        // walk from the back to the front, every time we get a jump > 1 we eval the point, if we get X consecutive
        // jumpts then we stop evaluating and give us finding the tail
        double lastVal = points[points.length - 1];
        SummaryStatistics jumps = new SummaryStatistics();
        int consecutiveIncreases = 0;
        for (int i = points.length - 2; i >= 0; i--) {
            double delta = points[i] - lastVal;
            lastVal = points[i];
            if (delta == 0) {
                consecutiveIncreases = 0;
                continue;
            }
            double thresh = Double.POSITIVE_INFINITY;
            if (jumps.getN() > 2) {
                thresh = jumps.getMean() + (10 * jumps.getStandardDeviation());
            }
            if (delta >= 1) {
                T candidate = distMaker.apply(i + 1);
                double score = scorer.applyAsDouble(candidate);
                minimizer.tryUpdate(candidate, score);
                jumps.addValue(delta);
            }
            consecutiveIncreases += 1;
            if (consecutiveIncreases > 50 || delta > thresh) {
                log.debug("Breaking early from fit. increases {}, delta {}", consecutiveIncreases, delta);
                break;
            }
        }
        return minimizer.getBest();
    }

    private TwoLinearDist makeTwoLinearAt(int split) {
        double slope = 0;
        double inter = 0;
        double tailSlope = 0;
        double tailInter = 0;

        if (split == 1) {
            inter = points[0];
        } else if (split >= 2) {
            SimpleRegression firstReg = linearRegressionOnSubset(0, split);
            slope = firstReg.getSlope();
            inter = firstReg.getIntercept();
        }
        if (split == points.length - 1) {
            tailInter = points[points.length - 1];
        } else if (split <= points.length - 2) {
            SimpleRegression secReg = linearRegressionOnSubset(split, points.length);
            tailSlope = secReg.getSlope();
            tailInter = secReg.getIntercept();
        }
        return new TwoLinearDist(split, slope, inter, tailSlope, tailInter);
    }

    private SimpleRegression linearRegressionOnSubset(int startIncl, int endExcl) {
        SimpleRegression regression = new SimpleRegression(true);
        for (int i = startIncl; i < endExcl; i++) {
            regression.addData(i, points[i]);
        }
        return regression;
    }

    private TwoLinearDist makeLinearPlusTailAt(int split) {
        double tailIcept = 0;
        int count = 0;
        for (int i = split; i < points.length; i++) {
            tailIcept += points[i];
            count += 1;
        }
        if (count > 0) {
            tailIcept /= (double) count;
        }
        if (split == 0) {
            return new TwoLinearDist(split, 0, 0, 0.0, tailIcept);
        }
        if (split == 1) {
            return new TwoLinearDist(split, 0, points[0], 0.0, tailIcept);
        }
        // do regression to find linear approx (prob switch to exp)
        SimpleRegression regression = linearRegressionOnSubset(0, split);
        return new TwoLinearDist(split, regression.getSlope(), regression.getIntercept(), 0.0, tailIcept);
    }

    private static PowerDist powerRegOrig(Iterable<DoublePair> inputXy) {
        double m = 0;
        double xbarNum = 0;
        // loop 1 get xbar
        for (DoublePair pair : inputXy) {
            double x = pair.getX();
            double y = pair.getY();

            double yy = y * y;
            m += yy;
            xbarNum += (yy * log(x));
        }
        double xbar = xbarNum / m;

        // loop 2 get t
        double t = 0;
        double bNum = 0;
        double aNum = 0;
        for (DoublePair pair : inputXy) {
            double x = pair.getX();
            double y = pair.getY();

            double yy = y * y;
            t += (yy * pow(log(x) - xbar, 2));
            bNum += (yy * log(y) * (log(x) - xbar));
            aNum += (yy * log(y));
        }
        double b = bNum / t;
        double a = exp((aNum / m) - (b * xbar));
        return new PowerDist(a, b, 0, Integer.MAX_VALUE, 0);
    }

    private PowerDist makePowerTailAt(int split) {
        final Iterable<DoublePair> inputXy = DoublePair.ranked(new DoubleArrayList(this.points));
        double tailIcept = 0;
        int count = 0;
        for (int i = split; i < points.length; i++) {
            tailIcept += points[i];
            count += 1;
        }
        if (count > 0) {
            tailIcept /= (double) count;
        }
        if (split == 0) {
            return new PowerDist(0, 0, 0, split, tailIcept);
        }
        if (split == 1) {
            return new PowerDist(points[0], 0, 0, split, tailIcept);
        }
        // at least two points to fit
        Iterable<DoublePair> limit = Iterables.limit(inputXy, split);
        PowerDist firstPart = fitPowerToIter(limit);
        return new PowerDist(firstPart.a, firstPart.b, firstPart.yCorrect, split, tailIcept);
    }

    private TwoPowerDist makeTwoPowerAt(int split) {
        final List<DoublePair> inputXy = Lists.newArrayList(DoublePair.ranked(new DoubleArrayList(this.points)));
        PowerDist first = null;
        PowerDist second = null;

        if (split == 1) {
            first = new PowerDist(points[0], 0.0, 0.0, 0, 0);
        } else if (split >= 2) {
            Iterable<DoublePair> limit = inputXy.subList(0, split);
            first = fitPowerToIter(limit);
        }
        if (split == points.length - 1) {
            second = new PowerDist(points[points.length - 1], 0.0, 0.0, 0, 0);
        } else if (split <= points.length - 2) {
            List<DoublePair> limit = inputXy.subList(split, points.length);
            second = fitPowerToIter(limit);
        }
        return new TwoPowerDist(first, second, split);
    }

    private PowerDist fitPowerToIter(Iterable<DoublePair> inputXy) {
        double min = Guavate.stream(inputXy).mapToDouble(DoublePair::getY).min().getAsDouble();
        double yCorrect = min - 1.0;
        inputXy = Guavate.stream(inputXy)
                .map(dp -> new DoublePair(dp.getX(), dp.getY() - yCorrect))
                .collect(Collectors.toList());

        UnivariateFunction gradientPowerRegression = makePowerGradiantFn(inputXy);

        // uses solver to improve on the weighted linear regression in power2
        PowerDist weightedLinear = powerRegOrig(inputXy);
        double bbar = weightedLinear.b;
        Preconditions.checkState(Doubles.isFinite(bbar), "not finite bbar", bbar);
        double[] bracket = UnivariateSolverUtils.bracket(gradientPowerRegression, bbar, bbar - 50, bbar + 50, 1_000);

        BrentSolver solver = new BrentSolver();
        double newB = solver.solve(700, gradientPowerRegression, bracket[0], bracket[1], bbar);

        // get new a based on new b
        double xby = 0;
        double x2b = 0;
        for (DoublePair pair : inputXy) {
            double x = pair.getX();
            double y = pair.getY();

            xby += (pow(x, newB) * y);
            x2b += (pow(x, 2 * newB));
        }

        double newA = xby / x2b;
        return new PowerDist(newA, newB, yCorrect, Integer.MAX_VALUE, 0);
    }

    private static UnivariateFunction makePowerGradiantFn(final Iterable<DoublePair> inputXy) {
        return b -> {
            double xby = 0;
            double lnx2b = 0;
            double x2b = 0;
            double lnxxby = 0;
            for (DoublePair pair : inputXy) {
                double x = pair.getX();
                double y = pair.getY();

                xby += (pow(x, b) * y);
                lnx2b += (log(x) * pow(x, 2 * b));
                x2b += (pow(x, 2 * b));
                lnxxby += (log(x) * pow(x, b) * y);
            }

            return (xby * lnx2b) - (x2b * lnxxby);
        };
    }

    public static class TwoPowerDist implements FitDist {
        private final PowerDist first;
        private final PowerDist second;
        private final int split;
        private final double minFirstVal;

        public TwoPowerDist(PowerDist first, PowerDist second, int split) {
            this.first = first;
            this.second = second;
            this.split = split;
            if (split >= 1) {
                minFirstVal = first.predict(split - 1);
            } else {
                minFirstVal = Double.POSITIVE_INFINITY;
            }
        }

        @Override
        public double predict(int zeroBasedRank) {
            if (zeroBasedRank >= split) {
                // i tried shiting the second distribution to make it 1-based but that
                // worked terribly with my empirical distributions
                return min(minFirstVal, second.predict(zeroBasedRank));
            }
            return first.predict(zeroBasedRank);
        }

        @Override
        public String toString() {
            return "TwoPowerDist{" +
                    "first=" + first +
                    ", second=" + second +
                    ", split=" + split +
                    '}';
        }
    }

    public static class PowerDist implements FitDist {

        final double a;
        final double b;
        final double yCorrect;
        final int splitPoint;
        final double tailIntercept;

        public PowerDist(double a, double b, double yCorrect, int splitPoint, double tailIntercept) {
            this.a = a;
            this.b = b;
            this.yCorrect = yCorrect;
            this.splitPoint = splitPoint;
            this.tailIntercept = tailIntercept;
        }

        @Override
        public double predict(int zeroBasedRank) {
            if (zeroBasedRank >= splitPoint) {
                return tailIntercept;
            }
            return max(1.0, yCorrect + (a * pow(zeroBasedRank + 1, b)));
        }

        public double calcInflectionPoint() {
            UnivariateFunction firstDeriv = x -> (a * b * pow(x, b - 1)) + 1;

            double[] bracket = UnivariateSolverUtils.bracket(firstDeriv, 100, 0.1, 100000, 1000);

            BrentSolver solver = new BrentSolver();
            double rank = solver.solve(100, firstDeriv, bracket[0], bracket[1]);
            return rank;
        }

        @Override
        public String toString() {
            return "PowerDist{" +
                    "a=" + a +
                    ", b=" + b +
                    ", yCorrect=" + yCorrect +
                    ", splitPoint=" + splitPoint +
                    ", tailIntercept=" + tailIntercept +
                    '}';
        }
    }

    public static class TwoLinearDist implements FitDist {

        private final int split;

        private final double slopeA;
        private final double intA;
        private final double minFirstVal;

        private final double slopeB;
        private final double intB;

        public TwoLinearDist(int split, double slopeA, double intA, double slopeB, double intB) {
            this.split = split;
            this.slopeA = slopeA;
            this.intA = intA;
            this.slopeB = slopeB;
            this.intB = intB;

            if (split >= 1) {
                this.minFirstVal = predict(split - 1);
            } else {
                this.minFirstVal = Double.POSITIVE_INFINITY;
            }
        }

        @Override
        public double predict(int zeroBasedRank) {
            if (zeroBasedRank < split) {
                return max(1.0, (slopeA * zeroBasedRank) + intA);
            }
            return min(minFirstVal, max(1.0, (slopeB * zeroBasedRank) + intB));
        }

        @Override
        public String toString() {
            return "TwoLinearDist{" +
                    "split=" + split +
                    ", slopeA=" + slopeA +
                    ", intA=" + intA +
                    ", slopeB=" + slopeB +
                    ", intB=" + intB +
                    '}';
        }
    }

    /*
    I never got the zipf fitting working, pretty sure my math is right for the likelihood function, not
    sure if im using the opto stuff correctly though; or zipf is just a really bad fit

    public static class ZipfDist implements FitDist {

            private final double negs;
            private final int n;

            public ZipfDist(double s, int n) {
                this.negs = -s;
                this.n = n;
            }

            @Override
            public double predict(int zeroBasedRank) {
                return max(0.0, pow(zeroBasedRank + 1, negs));
            }

            public double denom() {
                return IntStream.rangeClosed(1, n).mapToDouble(this::predict).sum();
            }

            @Override
            public String toString() {
                return "ZipfDist{" +
                        "s=" + -negs +
                        ", n=" + n +
                        '}';
            }
        }

        public ZipfDist fitZipf() {
            double[] logprobs = pointsToLogProbs();
            LeastSquaresProblem lsp = new LeastSquaresBuilder()
                    .model(makeOptoFn(logprobs))
                    .start(new double[]{1.04})
                    .target(logprobs)
                    .maxEvaluations(10_000)
                    .maxIterations(10_000)
                    .checker(new EvaluationRmsChecker(1e-3))
                    .build();

            LeastSquaresOptimizer opto = new GaussNewtonOptimizer()
                    .withDecomposition(Decomposition.SVD);
            Optimum optoResult = opto.optimize(lsp);
            RealVector point = optoResult.getPoint();
            double s = point.getEntry(0);
            log.debug("Finished fitting zipf s = " + s + " iterations " + optoResult.getIterations());
            return new ZipfDist(s, logprobs.length);
        }

        private MultivariateJacobianFunction makeOptoFn(double[] logprobs) {
            return (ps) -> {
                double s = ps.getEntry(0);
                log.debug("Evaluating zipf at {}", s);
                double negs = -s;
                RealVector predicted = new ArrayRealVector(logprobs.length);
                RealMatrix derivs = new Array2DRowRealMatrix(logprobs.length, 1);

                double denomA = IntStream.rangeClosed(1, logprobs.length).mapToDouble(i -> pow(i, negs)).sum();
                double denomB = IntStream.rangeClosed(1, logprobs.length).mapToDouble(i ->
                        log(i) * pow(i, negs) * -1).sum();
                double dsdyPart = ((1.0 / denomA) * denomB);
                double denom = IntStream.rangeClosed(1, logprobs.length).mapToDouble(rnk -> zipf(s, rnk)).sum();
                double logDenom = log(denom);

                IntStream.rangeClosed(1, logprobs.length).forEach(i -> {
                    double predict = logzipf(s, i) - logDenom;
                    double deriv = Math.log(i) + dsdyPart;
                    Preconditions.checkState(Doubles.isFinite(predict), "nan predict");
                    Preconditions.checkState(Doubles.isFinite(deriv), "nan deriv");
                    predicted.setEntry(i - 1, predict);
                    derivs.setEntry(i - 1, 0, deriv);
                });
                return Pair.create(predicted, derivs);
            };
        }

        private double zipf(double s, int rank) {
            return pow(rank, -s);
        }

        private double logzipf(double s, int rank) {
            return -s * log(rank);
        }

        private double[] pointsToLogProbs() {
            double[] p = new double[points.length];
            double logsum = log(Arrays.stream(points).sum());
            for (int i = 0; i < points.length; i++) {
                p[i] = log(points[i]) - logsum;
            }
            return p;
        }
        */
}
