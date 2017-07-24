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

package com.github.steveash.synthrec.sampling;

/**
 * Owns the DAG representing the hierarchical model and the multinomials underneath. This takes a
 * 1) CountDAG
 * 2) Dist Priors (domain knowledge to help sparsity problems)
 * 3) hyper-priors (general smoothing via Dirichlet priors)
 * 4) de-identification via substitution
 * @author Steve Ash
 */
public class SamplingDag {
}
