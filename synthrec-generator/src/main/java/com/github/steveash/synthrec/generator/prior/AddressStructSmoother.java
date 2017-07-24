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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.address.AddressTag;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.stat.EmpPriorSmoother;
import com.github.steveash.synthrec.stat.MutableMultinomial;
import com.github.steveash.synthrec.stat.Sampler;
import com.github.steveash.synthrec.stat.SamplingTable;
import com.google.common.collect.Maps;

/**
 * Takes a map of subfield (i.e. sketch field) -> values (empirical/observed) and smooths them (like a prior)
 * then converts them to a sampling table
 * @author Steve Ash
 */
@LazyComponent
public class AddressStructSmoother {
    private static final Logger log = LoggerFactory.getLogger(AddressStructSmoother.class);

    @Resource private GenRecordsConfig genRecordsConfig;

    /**
     * Takes the given map of address tags -> values (e.g. entries like PREDIRECTIONAL -> [W, E, N, S]) and
     * mutates this to add a prior count based on known gazetters (see AddressCounts) then returns the map
     * of the smoothed multinomials as sampling tables
     * note that the map passed in will be mutated
     * @param addressTagToValuesToMutate
     * @return
     */
    public Map<String, Sampler<?>> smooth(Map<String, MutableMultinomial<Object>> addressTagToValuesToMutate) {

        HashMap<String, Sampler<?>> result = Maps.newHashMap();
        EmpPriorSmoother smoother = new EmpPriorSmoother(genRecordsConfig.getAddressPriorAlpha(),
                genRecordsConfig.getAddressMinVirtual());

        for (Entry<String, MutableMultinomial<Object>> entry : addressTagToValuesToMutate.entrySet()) {
            MutableMultinomial<?> smoothed = smoothOne(entry.getKey(),
                    entry.getValue(),
                    smoother
            );
            SamplingTable<?> sampler = SamplingTable.createFromMultinomial(smoothed);
            result.put(entry.getKey(), sampler);
        }
        return result;
    }

    private <T> MutableMultinomial<T> smoothOne(String addressTag,
            MutableMultinomial<T> multi,
            EmpPriorSmoother smoother
    ) {
        AddressTag tag = AddressTag.valueOf(addressTag);

        if (AddressCounts.TAG_TO_RESOURCE.keySet().contains(tag)) {
            MutableMultinomial<T> priorMulti = (MutableMultinomial) AddressCounts.loadMultinomialForTag(tag);
            smoother.smoothPriorCopy(multi, priorMulti, tag.name());
            return priorMulti;
        }
        // for the others not in the address counts, we have some special rules
        switch (tag) {
            case Recipient:
            case CoObject:
                throw new IllegalStateException("Dont support " + tag + " in the structure yet");
            default:
                // city, state, etc,
                // anything else shouldn't show up and is a bug if it does
                throw new IllegalStateException("shouldnt see " + tag + " in an address struct");
        }
    }
}
