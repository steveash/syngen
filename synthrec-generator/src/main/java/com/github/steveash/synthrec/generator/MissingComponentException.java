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

package com.github.steveash.synthrec.generator;

/**
 * @author Steve Ash
 */
public class MissingComponentException extends RuntimeException {

    public static MissingComponentException makeForComponent(String component, String springBean) {
        return new MissingComponentException("Missing a component needed to run de-identification; you need to " +
                "provide a " + component + " because the framework does not have a default and you are using " +
                "a node/feature that requires this; once you have one you need to register a spring bean named " +
        springBean);
    }

    public MissingComponentException() {
    }

    public MissingComponentException(String message) {
        super(message);
    }
}
