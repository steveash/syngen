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

import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

/**
 * Box that keeps a score value and the best (by lowest score) is kept
 * @author Steve Ash
 */
public class BestBox<T> {

    public static <T> BestBox<T> maximizer() {
        return new BestBox<T>(OptimizeType.Maximize);
    }

    public static <T> BestBox<T> minimizer() {
        return new BestBox<T>(OptimizeType.Minimize);
    }

    public BestBox(OptimizeType optoType) {this.optoType = optoType;}

    public enum OptimizeType { Maximize, Minimize };

    private final OptimizeType optoType;
    private double score;
    private T best;

    public void setBest(T newBest, double newCost) {
        this.score = newCost;
        this.best = newBest;
    }

    public T tryUpdate(T candidate, ToDoubleFunction<T> scoreCalculator) {
        double score = scoreCalculator.applyAsDouble(candidate);
        return tryUpdate(candidate, score);
    }

    public T tryUpdate(T candidate, double candidateCost) {
        if (best == null) {
            best = candidate;
            score = candidateCost;
        }
        if (optoType == OptimizeType.Maximize && candidateCost > score) {
            score = candidateCost;
            best = candidate;
        } else if (optoType == OptimizeType.Minimize && candidateCost < score) {
            score = candidateCost;
            best = candidate;
        }
        return best;
    }

    public T getBest() {
        return best;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "BestBox{" +
                "score=" + score +
                ", best=" + best +
                '}';
    }
}
