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

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.google.common.collect.Lists;

/**
 * @author Steve Ash
 */
@LazyComponent
@ConfigurationProperties("synthrec.gen.records")
public class GenRecordsConfig {

    private int minCountAnonymity = 30;
    private int produceCount;
    private int maxRejectSamples = 1000;
    private double defaultPriorAlpha = 0.01;
    private double defaultPriorMinVirtual = 500;
    private boolean conditionalOnlyEmitCommonEntries = false;
    private boolean conditionalGivenNameOnlyEmitCommon = false;
    private boolean conditionalFamilyNameOnlyEmitCommon = false;

    // field specific prior hyper parameters
    private double agePriorAlpha = 0.01;
    private double agePriorMinVirtual = 500;
    private double zipPriorAlpha = 0.01;
    private double zipPriorMinVirtual = 500;
    private double addressPriorAlpha = 0.01;
    private double addressMinVirtual = 500;
    private double nameCultureMinProb = 0.10;
    private int nameCultureMinEntries = 250; // if fewer than this then don't sample from it, just backoff

    private List<String> goldFields = Lists.newArrayList();

    public int getMinCountAnonymity() {
        return minCountAnonymity;
    }

    public void setMinCountAnonymity(int minCountAnonymity) {
        this.minCountAnonymity = minCountAnonymity;
    }

    public int getProduceCount() {
        return produceCount;
    }

    public void setProduceCount(int produceCount) {
        this.produceCount = produceCount;
    }

    public double getDefaultPriorAlpha() {
        return defaultPriorAlpha;
    }

    public void setDefaultPriorAlpha(double defaultPriorAlpha) {
        this.defaultPriorAlpha = defaultPriorAlpha;
    }

    public double getAgePriorAlpha() {
        return agePriorAlpha;
    }



    public void setAgePriorAlpha(double agePriorAlpha) {
        this.agePriorAlpha = agePriorAlpha;
    }

    public List<String> getGoldFields() {
        return goldFields;
    }

    public int getMaxRejectSamples() {
        return maxRejectSamples;
    }

    public void setMaxRejectSamples(int maxRejectSamples) {
        this.maxRejectSamples = maxRejectSamples;
    }

    public double getAddressPriorAlpha() {
        return addressPriorAlpha;
    }

    public void setAddressPriorAlpha(double addressPriorAlpha) {
        this.addressPriorAlpha = addressPriorAlpha;
    }

    public void setAddressMinVirtual(double addressMinVirtual) {
        this.addressMinVirtual = addressMinVirtual;
    }

    public double getAddressMinVirtual() {
        return addressMinVirtual;
    }

    public double getDefaultPriorMinVirtual() {
        return defaultPriorMinVirtual;
    }

    public boolean isConditionalOnlyEmitCommonEntries() {
        return conditionalOnlyEmitCommonEntries;
    }

    public void setConditionalOnlyEmitCommonEntries(boolean conditionalOnlyEmitCommonEntries) {
        this.conditionalOnlyEmitCommonEntries = conditionalOnlyEmitCommonEntries;
    }

    public boolean isConditionalGivenNameOnlyEmitCommon() {
        return conditionalGivenNameOnlyEmitCommon;
    }

    public void setConditionalGivenNameOnlyEmitCommon(boolean conditionalGivenNameOnlyEmitCommon) {
        this.conditionalGivenNameOnlyEmitCommon = conditionalGivenNameOnlyEmitCommon;
    }

    public boolean isConditionalFamilyNameOnlyEmitCommon() {
        return conditionalFamilyNameOnlyEmitCommon;
    }

    public void setConditionalFamilyNameOnlyEmitCommon(boolean conditionalFamilyNameOnlyEmitCommon) {
        this.conditionalFamilyNameOnlyEmitCommon = conditionalFamilyNameOnlyEmitCommon;
    }

    public void setDefaultPriorMinVirtual(double defaultPriorMinVirtual) {
        this.defaultPriorMinVirtual = defaultPriorMinVirtual;
    }

    public double getNameCultureMinProb() {
        return nameCultureMinProb;
    }

    public void setNameCultureMinProb(double nameCultureMinProb) {
        this.nameCultureMinProb = nameCultureMinProb;
    }

    public int getNameCultureMinEntries() {
        return nameCultureMinEntries;
    }

    public void setNameCultureMinEntries(int nameCultureMinEntries) {
        this.nameCultureMinEntries = nameCultureMinEntries;
    }

    public double getZipPriorAlpha() {
        return zipPriorAlpha;
    }

    public void setZipPriorAlpha(double zipPriorAlpha) {
        this.zipPriorAlpha = zipPriorAlpha;
    }

    public double getZipPriorMinVirtual() {
        return zipPriorMinVirtual;
    }

    public void setZipPriorMinVirtual(double zipPriorMinVirtual) {
        this.zipPriorMinVirtual = zipPriorMinVirtual;
    }

    public double getAgePriorMinVirtual() {
        return agePriorMinVirtual;
    }

    public void setAgePriorMinVirtual(double agePriorMinVirtual) {
        this.agePriorMinVirtual = agePriorMinVirtual;
    }
}
