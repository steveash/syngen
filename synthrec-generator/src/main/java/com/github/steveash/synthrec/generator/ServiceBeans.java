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

import javax.management.MBeanServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.jmx.JmxMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jmx.export.MBeanExporter;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.github.steveash.synthrec.deident.DeidentRecordService;
import com.github.steveash.synthrec.deident.DeidentifierRegistry;

/**
 * @author Steve Ash
 */
@Lazy
@Configuration
public class ServiceBeans {
    private static final Logger log = LoggerFactory.getLogger(ServiceBeans.class);

    @Profile("!unittest")
    @Lazy(false)
    @Bean
    @ExportMetricWriter
    MetricWriter metricWriter(MetricRegistry registry, MBeanExporter exporter, MBeanServer mBeanServer) {
        final JmxReporter reporter = JmxReporter.forRegistry(registry)
                .inDomain("com.github.steveash.synthrec")
                .registerWith(mBeanServer)
                .build();
        log.info("Starting JMX export of metrics...");
        reporter.start();
        return new JmxMetricWriter(exporter);
    }

    @Bean
    public DeidentRecordService deidentRecordService(DeidentifierRegistry deidentifierRegistry,
            MetricRegistry metricRegistry
    ) {
        return new DeidentRecordService(deidentifierRegistry, metricRegistry);
    }

}
