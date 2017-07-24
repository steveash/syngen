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

package com.github.steveash.synthrec.deident;

import static com.github.steveash.synthrec.deident.VocabDeidentifier.NULL_OBSERVER;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.count.CountDag.SensitiveDistrib;
import com.github.steveash.synthrec.deident.VocabDeidentifier.Observer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.Closer;

/**
 * Takes an input DAG which might have sensitive stuff marked in it; and deidents any sensitive info
 * @author Steve Ash
 */
public class DeidentRecordService {
    private static final Logger log = LoggerFactory.getLogger(DeidentRecordService.class);

    private final DeidentifierRegistry registry;
    private final MetricRegistry metrics;
    private final Meter deidentInputMarker;
    private final Meter deidentBlockingMarker;
    private final Meter deidentFirstPassMarker;

    public DeidentRecordService(DeidentifierRegistry registry, MetricRegistry metrics) {
        this.registry = registry;
        this.metrics = metrics;
        deidentInputMarker = metrics.meter("deidentInput");
        deidentBlockingMarker = metrics.meter("deidentBlocking");
        deidentFirstPassMarker = metrics.meter("deident1stPass");
    }

    public void deident(CountDag input, @Nullable String deidentOut) throws IOException {

        input.allSensitiveDistribs().forEach(dist -> {

            Closer closer = Closer.create();
            try {
                Observer obv = makeObserver(deidentOut, dist, closer);

                log.info("Deidentifying " + dist.distribName + ", " + dist.subFieldName);
                VocabDeidentifier<Object> deident = registry.deidentifierFor(dist.distribName,
                        dist.subFieldName
                );
                init(deident);
                deident.deidentify(dist.vocab, dist.counter::countByValue, obv);
            } finally {
                try {
                    closer.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    public void init(VocabDeidentifier<Object> deident) {
        if (deident instanceof KAnonDeidentifier) {
            KAnonDeidentifier<Object,Object> kanon = (KAnonDeidentifier<Object, Object>) deident;
            kanon.setDeidentMarker(this.deidentInputMarker::mark);
            kanon.setBlockingMarker(this.deidentBlockingMarker::mark);
            kanon.setFirstPassMarker(this.deidentFirstPassMarker::mark);
        }
    }

    private Observer makeObserver(@Nullable String deidentOut, SensitiveDistrib dist, Closer closer) {
        if (isBlank(deidentOut)) {
            return NULL_OBSERVER;
        }
        String name = deidentOut + "." + dist.distribName +
                (dist.subFieldName != null ? "." + dist.subFieldName : "") + ".log";

        // delay write out file since some things (most) dont need any deident
        Supplier<PrintWriter> supp = Suppliers.memoize(() -> {
            try {
                return closer.register(new PrintWriter(new FileOutputStream(name)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return new Observer() {
            @Override
            public void onBlockingReplace(Object sensitive, Object replacement) {
                supp.get().println("B: " + sensitive + " -> " + replacement);
            }

            @Override
            public void onSampleReplace(Object sensitive, Object replacement) {
                supp.get().println("S: " + sensitive + " -> " + replacement);
            }
        };
    }
}
