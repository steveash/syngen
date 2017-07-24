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

package com.github.steveash.synthrec.generator.prior;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.Constants;
import com.github.steveash.synthrec.collect.LazyMap;
import com.github.steveash.synthrec.domain.AssignmentInstance;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.socio.ZipData;
import com.github.steveash.synthrec.socio.ZipDataLookup;
import com.github.steveash.synthrec.stat.BackoffSampler;
import com.github.steveash.synthrec.generator.feature.CityBinFeature;
import com.github.steveash.synthrec.generator.feature.CityBinFeature.CityBin;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.ConditionalSampler;
import com.github.steveash.synthrec.stat.EmpPriorSmoother;
import com.github.steveash.synthrec.stat.Multinomial;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * Smoother that can do zip code/population related smoothing
 * @author Steve Ash
 */
@LazyComponent
public class ZipSmoother {
    private static final Logger log = LoggerFactory.getLogger(ZipSmoother.class);

    @Resource private GenRecordsConfig genRecordsConfig;
    @Resource private CityBinFeature cityBinFeature;
    @Resource private ZipDataLookup zipDataLookup;

    public SamplingTable<String> smoothStates(MutableMultinomial<String> distToSmooth) {

        EmpPriorSmoother smoother = new EmpPriorSmoother(genRecordsConfig.getZipPriorAlpha(), genRecordsConfig.getZipPriorMinVirtual());
        double virtualCount = smoother.calcVirtualCount(distToSmooth, cityBinFeature.statePopulations(),
                "us-state", true
        );
        return smoothStatesByVirtualCount(distToSmooth, virtualCount);
    }

    private SamplingTable<String> smoothStatesByVirtualCount(MutableMultinomial<String> distToSmooth,
            double virtualCount
    ) {
        MutableMultinomial<String> state = cityBinFeature.statePopulations().copy();
        state.scaleToVirtualCount(virtualCount);
        distToSmooth.addMultinomial(state);
        return SamplingTable.createFromMultinomial(distToSmooth);
    }

    public BackoffSampler<CityBin> smoothCity(
            Map<AssignmentInstance, MutableMultinomial<CityBin>> distToSmooth,
            Set<String> factorParents
    ) {
        EmpPriorSmoother smoother = new EmpPriorSmoother(genRecordsConfig.getZipPriorAlpha(), genRecordsConfig.getZipPriorMinVirtual());
        Map<AssignmentInstance, Sampler<CityBin>> map = Maps.newHashMap();
        MutableMultinomial<CityBin> backoff = new MutableMultinomial<>(-1);

        for (Entry<AssignmentInstance, MutableMultinomial<CityBin>> entry : distToSmooth.entrySet()) {
            MutableMultinomial<CityBin> thisMulti = entry.getValue();
            String state = (String) entry.getKey().get(Constants.ADDRESS_STATE, null);
            Preconditions.checkNotNull(state, "cant be null to smooth state", entry.getKey());
            Multinomial<CityBin> maybe = cityBinFeature.cityDistributionForState(state);
            if (maybe != null) {
                MutableMultinomial<CityBin> copy = maybe.copy();
                smoother.smoothPriorCopy(thisMulti, copy, "state-citysize");
                thisMulti = copy; // use the smoothed version
                backoff.addMultinomial(copy);
            }
            map.put(entry.getKey(), SamplingTable.createFromMultinomial(thisMulti));
        }
        Sampler<CityBin> sampler = SamplingTable.createFromMultinomial(backoff);
        return new BackoffSampler<>(map, ConditionalSampler.adaptSampler(sampler), factorParents);
    }

    public Map<Pair<String,CityBin>, SamplingTable<String>> makeZipConditional() {
        LazyMap<Pair<String,CityBin>, MutableMultinomial<String>> aggregator = LazyMap.makeSupply(() -> new MutableMultinomial<String>(-1));
        int zeroPop = 0;
        int gtZeroPop = 0;
        for (ZipData zipData : zipDataLookup.allZips()) {
            double pop = zipData.getEstimatedPopulation();
            if (zipData.getEstimatedPopulation() <= 0) {
                zeroPop += 1;
                pop = 1.0;
            } else {
                gtZeroPop += 1;
            }
            CityBin bin = CityBinFeature.getCityBin(zipData);
            MutableMultinomial<String> multi = aggregator.get(Pair.of(zipData.getState(), bin));
            multi.add(zipData.getZipcode(), pop);
        }
        log.info("Added up zips for a conditional sampling distribution; " + zeroPop + " zips had " +
                "zero pop; " + gtZeroPop + " had gt zero pop");
        return ImmutableMap.copyOf(Maps.transformValues(aggregator, SamplingTable::createFromMultinomial));
    }
}
