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

package com.github.steveash.synthrec.domain;

import static javafx.scene.input.KeyCode.T;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;

import com.github.steveash.synthrec.canonical.Normalizers;
import com.github.steveash.synthrec.domain.FeatureComputer.FeatureKey;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * Represents a single representation of a demographic record; allows for extensible mapping
 * as well as caching for features and calculated normal values (phonetic values, etc.)
 * @author Steve Ash
 */
public class Record implements ReadableRecord, WriteableRecord {

    private final int recordId;
    private final Map<String, String> fields = Collections.synchronizedMap(Maps.newHashMap());
    private final Map<String, String> normalFields = Collections.synchronizedMap(Maps.newHashMap());
    private final Map<String, Object> features = Collections.synchronizedMap(Maps.newHashMap());

    public Record(int recordId) {this.recordId = recordId;}

    public void setInitialValue(String header, String value) {
        if (isNotBlank(value)) {
            fields.put(Normalizers.interner().intern(header), Normalizers.interner().intern(value));
        }
    }

    @Override
    public String getField(String header, String defaultValue) {
        return fields.getOrDefault(header, defaultValue);
    }

    @Override
    public void setNormal(String header, @Nullable String value) {
        if (value != null) {
            normalFields.put(Normalizers.interner().intern(header), Normalizers.interner().intern(value));
        }
    }

    @Override
    public String getNormal(String header, String defaultValue) {
        return normalFields.getOrDefault(header, defaultValue);
    }

    @Override
    public Map<String,String> fields() {
        return fields;
    }

    @Override
    public <T> T getFeature(FeatureKey<T> featureKey, T defaultValue) {
        String key = featureKey.getKey();
        return getFeatureByKey(key, defaultValue);
    }

    public <T> T getFeatureByKey(String key, T defaultValue) {
        Object result = features.getOrDefault(key, defaultValue);
        if (result instanceof FeatureThunk) {
            // dont race since the thunk is mutating
            synchronized (this) {
                result = features.getOrDefault(key, defaultValue);

                if (!(result instanceof FeatureThunk)) {
                    return (T) result;
                }
                ((FeatureThunk) result).calculate(this);
            }
            result = features.getOrDefault(key, defaultValue);
        }
        return (T) result;
    }

    @Override
    public <T> void setFeature(FeatureKey<T> featureKey, T featureValue) {
        if (featureValue == null) {
            return;
        }
        if (featureValue instanceof CharSequence) {
            if (isBlank((CharSequence) featureValue)) {
                return;
            }
            featureValue = (T) Normalizers.interner().intern(featureValue.toString());
        }
        Object prevValue = features.put(featureKey.getKey(), featureValue);
        Preconditions.checkState(prevValue == null, "cant re-associate a feature value for the same key");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Record record = (Record) o;

        return recordId == record.recordId;
    }

    @Override
    public int hashCode() {
        return recordId;
    }

    @Override
    public String toString() {
        return "Record{" +
                "recordId=" + recordId +
                ", fields=" + fields +
                '}';
    }
}
