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

package com.github.steveash.synthrec.generator.gen;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.steveash.synthrec.count.CountDag;
import com.github.steveash.synthrec.gen.GenNode;
import com.github.steveash.synthrec.gen.GenNodeProvider;
import com.github.steveash.synthrec.generator.GenRecordsConfig;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.Maps;

/**
 * Factory that knows how to build gen nodes (either normal ones or smoothed ones from CountDag nodes)
 * @author Steve Ash
 */
@LazyComponent
public class GenNodeRegistry {
    private static final Logger log = LoggerFactory.getLogger(GenNodeRegistry.class);

    // this is all of the gen nodes that are registered with spring
    @Autowired private Collection<GenNode> springGenNodes;
    @Autowired private Collection<GenNodeProvider> springGenNodeProviders;

    @Resource private GenRecordsConfig genRecordsConfig;

    private Map<String, GenNode> genNodesByOutputField;
    private Map<String, GenNodeProvider> providersByName;

    @PostConstruct
    protected void setup() {
        genNodesByOutputField = Maps.newHashMapWithExpectedSize(springGenNodes.size());
        for (GenNode springGenNode : springGenNodes) {
            for (String outputKey : springGenNode.outputKeys()) {
                if (genNodesByOutputField.containsKey(outputKey)) {
                    throw new RuntimeException("you have multiple beans configured in the contain to generate" +
                            "output field " + outputKey);
                }
                genNodesByOutputField.put(outputKey, springGenNode);
            }
        }
        log.info("Loaded " + genNodesByOutputField.size() + " generators from the container");

        providersByName = Maps.newHashMapWithExpectedSize(springGenNodeProviders.size());
        for (GenNodeProvider provider : springGenNodeProviders) {
            for (String name : provider.providesForNames()) {
                if (providersByName.containsKey(name)) {
                    throw new RuntimeException("you have multiple provider beans configured in the contain to generate" +
                            " for " + name);
                }
                if (genNodesByOutputField.containsKey(name)) {
                    throw new RuntimeException("you have a provider for " + name + " and a singleton gennode in the " +
                            "container to provide for the same field; they cant overlap. I dont know which to use");
                }
                providersByName.put(name, provider);
            }
        }
        log.info("Loaded " + providersByName.size() + " providers for generators from the container");
    }

    public GenNode makeNodeFor(String factorName, CountDag dag) {
        GenNodeProvider provider = providersByName.get(factorName);
        if (provider != null) {
            return provider.makeFor(factorName, dag);
        }
        GenNode genNode = genNodesByOutputField.get(factorName);
        if (genNode == null) {
            throw new IllegalArgumentException("No generator knows how to generate for field " + factorName);
        }
        return genNode;
    }
}
