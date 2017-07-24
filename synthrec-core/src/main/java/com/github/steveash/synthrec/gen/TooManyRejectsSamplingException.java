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
 * Indicates that sampling cant proceed for one reason or another; maybe too many rejections or invalid state
 * @author Steve Ash
 */
public class TooManyRejectsSamplingException extends RuntimeException {

    private static final long serialVersionUID = -1458886496419182013L;

    public TooManyRejectsSamplingException() {
        this("Tried to generate a sample many times but they were all rejected");
    }

    public TooManyRejectsSamplingException(String message) {
        super(message);
    }

    public TooManyRejectsSamplingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooManyRejectsSamplingException(Throwable cause) {
        super(cause);
    }
}
