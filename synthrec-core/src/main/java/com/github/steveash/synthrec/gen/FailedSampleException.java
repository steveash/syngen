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

package com.github.steveash.synthrec.gen;

/**
 * When you cant sample and dont know why and want to kill the whole process; this is not Too Many rejections
 * where retrying might be the right policy
 * @see TooManyRejectsSamplingException
 * @author Steve Ash
 */
public class FailedSampleException extends RuntimeException {

    public FailedSampleException() {
    }

    public FailedSampleException(String message) {
        super(message);
    }

    public FailedSampleException(String message, Throwable cause) {
        super(message, cause);
    }
}
