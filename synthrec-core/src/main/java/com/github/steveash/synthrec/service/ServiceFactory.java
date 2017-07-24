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

package com.github.steveash.synthrec.service;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A loader for specific "roll your own" implementations that are resolved from the
 * class path (like proprietary name parsers, etc.)
 * @author Steve Ash
 */
public class ServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(ServiceFactory.class);

    public <T, F extends Supplier<T>> T locateFromFactory(Class<F> factoryClass) {
        F factory = locate(factoryClass);
        try {
            return factory.get();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Trying to locate extension point " + factoryClass +
                    " threw an exception trying to instantiate the instance", e);
        }
    }

    public <T> T locate(Class<T> serviceClass) {
        ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
        Iterator<T> iter = loader.iterator();
        T defaultImpl = null;
        T pickedService = null;
        while (iter.hasNext()) {
            T service = iter.next();
            if (service.getClass().getAnnotation(DefaultService.class) != null) {
                defaultImpl = service;
            } else if (pickedService == null) {
                // this is not annotated, so pick it if first
                pickedService = service;
            } else {
                log.debug("Detected service {} but ignoring it since the first service found was {}",
                        service, pickedService);
            }
        }
        if (pickedService == null) {
            if (defaultImpl != null) {
                return defaultImpl;
            }
            throw new IllegalArgumentException("Cannot locate a service on the classpath for " + serviceClass);
        }
        return pickedService;
    }
}
