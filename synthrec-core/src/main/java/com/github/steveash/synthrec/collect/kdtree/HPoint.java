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
package com.github.steveash.synthrec.collect.kdtree;

// Hyper-Point class supporting KDTree class

class HPoint {

    protected double[] coord;

    protected HPoint(int n) {
        coord = new double[n];
    }

    protected HPoint(double[] x) {

        coord = new double[x.length];
        for (int i = 0; i < x.length; ++i)
            coord[i] = x[i];
    }

    protected Object clone() {

        return new HPoint(coord);
    }

    protected boolean equals(HPoint p) {

        // seems faster than java.util.Arrays.equals(), which is not
        // currently supported by Matlab anyway
        for (int i = 0; i < coord.length; ++i)
            if (coord[i] != p.coord[i])
                return false;

        return true;
    }

    protected static double sqrdist(HPoint x, HPoint y) {

        double dist = 0;

        for (int i = 0; i < x.coord.length; ++i) {
            double diff = (x.coord[i] - y.coord[i]);
            dist += (diff * diff);
        }

        return dist;

    }

    protected static double eucdist(HPoint x, HPoint y) {

        return Math.sqrt(sqrdist(x, y));
    }

    public String toString() {
        String s = "";
        for (int i = 0; i < coord.length; ++i) {
            s = s + coord[i] + " ";
        }
        return s;
    }

}
