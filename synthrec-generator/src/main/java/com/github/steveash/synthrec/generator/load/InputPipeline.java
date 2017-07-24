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

package com.github.steveash.synthrec.generator.load;

import java.util.stream.Stream;

import javax.annotation.Resource;

import org.springframework.context.annotation.Lazy;

import com.github.steveash.synthrec.domain.Record;
import com.github.steveash.synthrec.generator.enrich.FeatureService;
import com.github.steveash.synthrec.generator.enrich.NormalizerService;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.Streams;

/**
 * @author Steve Ash
 */
@LazyComponent
public class InputPipeline {

    @Lazy @Resource private NormalizerService normalizerService;
    @Lazy @Resource private FeatureService featureService;

    public Stream<Record> rawFrom(InputFile file) {
        return Streams.stream(file);
    }

    public Stream<Record> normalizedFrom(InputFile file) {
        return rawFrom(file).map(normalizerService::normalize);
    }

    public Stream<Record> processedFrom(InputFile file) {
        return normalizedFrom(file).map(featureService::enrichRecord);
    }
}
