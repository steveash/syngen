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

package com.github.steveash.synthrec.generator.feature;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.domain.ReadableRecord;
import com.github.steveash.synthrec.domain.SingleFeatureComputer;
import com.github.steveash.synthrec.domain.WriteableRecord;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.socio.ZipData;
import com.github.steveash.synthrec.socio.ZipDataLookup;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;

/**
 * Returns a moniker indicating if this is a large, medium, or small city -- based on the zip code database
 * @author Steve Ash
 */
@LazyComponent
public class CityBinFeature extends SingleFeatureComputer {

    private static final int BIG_CITY = 1000000;
    private static final int SMALL_CITY = 50000;

    private final ZipDataLookup zipDataLookup;
    private final Supplier<Map<String, ? extends Multinomial<CityBin>>> stateToBin;
    private final Supplier<Multinomial<String>> states;

    public enum CityBin {
        Small,
        Medium,
        Large
    }

    public static final FeatureKey<CityBin> CITY_BIN_FEATURE = new FeatureKey<>(Constants.ADDRESS_CITY_BIN,
            CityBin.class
    );

    @Autowired
    public CityBinFeature(ZipDataLookup zipDataLookup) {
        super(CITY_BIN_FEATURE);
        this.zipDataLookup = zipDataLookup;
        this.states = Suppliers.memoize(() -> {
            MutableMultinomial<String> states = new MutableMultinomial<>(-1);
            zipDataLookup.allZips().stream()
                    .filter(zi -> isNotBlank(zi.getFips()) && zi.getFipsPopulation() > 0)
                    .forEach(zi -> states.add(zi.getState().toUpperCase().trim().intern(), zi.getEstimatedPopulation()));
            return states;
        });
        this.stateToBin = Suppliers.memoize(() -> {
            Map<String, MutableMultinomial<CityBin>> states = Maps.newHashMapWithExpectedSize(75);
            Map<String, List<ZipData>> byState = zipDataLookup.allZips()
                    .stream()
                    .filter(zi -> zi.getFipsPopulation() > 0)
                    .collect(Collectors.groupingBy(zi -> zi.getState().toUpperCase()));

            for (Entry<String, List<ZipData>> entry : byState.entrySet()) {
                List<ZipData> allZips = entry.getValue();
                Map<String, Double> byFips = allZips.stream()
                        .filter(zi -> isNotBlank(zi.getFips()) && zi.getFipsPopulation() > 0)
                        .collect(Collectors.groupingBy(zi -> zi.getFips().toUpperCase(),
                                Collectors.averagingInt(ZipData::getFipsPopulation)
                        ));

                MutableMultinomial<CityBin> multi = new MutableMultinomial<>(CityBin.values().length);
                for (Double pop : byFips.values()) {

                    CityBin bin = CityBin.Medium;
                    if (pop > BIG_CITY) {
                        bin = CityBin.Large;
                    } else if (pop < SMALL_CITY) {
                        bin = CityBin.Small;
                    }
                    multi.add(bin, 1.0);
                }
                states.put(entry.getKey(), multi);
            }
            return states;
        });
    }

    @Override
    public void emitFeatures(ReadableRecord record, WriteableRecord sink) {
        String zip = record.getNormal(Constants.ADDRESS_ZIP, null);
        if (zip == null) {
            return;
        }
        Optional<ZipData> info = zipDataLookup.apply(zip);
        if (!info.isPresent()) {
            return;
        }
        ZipData zipData = info.get();
        CityBin bin = getCityBin(zipData);
        sink.setFeature(CITY_BIN_FEATURE, bin);
    }

    @Nullable
    public Multinomial<CityBin> cityDistributionForState(String state) {
        return this.stateToBin.get().get(state);
    }

    public Multinomial<String> statePopulations() {
        return this.states.get();
    }

    public static CityBin getCityBin(ZipData zip) {
        if (zip.getState().equals("AE") || zip.getState().equals("AA")) {
            return CityBin.Small;
        }
        int pop = zip.getFipsPopulation();
        CityBin bin = CityBin.Medium;
        if (pop > BIG_CITY) {
            bin = CityBin.Large;
        } else if (pop < SMALL_CITY) {
            bin = CityBin.Small;
        }
        return bin;
    }
}
