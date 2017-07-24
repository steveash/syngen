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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * A feature is a computer/emitter of transformed information.  The source input into the feature computer can
 * be a raw value, a normalized value, or another feature itself.  each feature computer emits one or more feature
 * values into the record. Feature values can be pulled out later by asking a record for a feature value via a
 * feature key.  The feature key is just identified as a string value -- but to make things convenient also
 * includes the feature value type (String, Integer, LocalDate, etc.).  Note that the string value _key_ is the
 * natural key -- not key + type.
 *
 * If feature computers use the output of other feature computers then it needs to report that via the
 * requries/satisfies methods so that the FeatureService can properly order the computer so that all
 * depedendencies are satisfied.  It will also check that there are no circular feature dependencies
 * @author Steve Ash
 */
public interface FeatureComputer {

    /**
     * actually emits feature values into the record; this might emit zero, one, or more feature values. each will
     * have their own keys.
     * @param record
     * @return
     */
    void emitFeatures(ReadableRecord record, WriteableRecord sink);

    /**
     * Advertise your requirements here; all of these feature values will need to already be emitted by other
     * computers (i.e. those that collectively satisfies() these requires())
     * @return
     */
    default Set<FeatureKey<?>> requires() {
        return ImmutableSet.of();
    }

    /**
     * This computer emits one or more feature values in the best case; while its true that not every record
     * will get every feature -- these constraints are from the perspective of the best case just so that the
     * feature service can order computers properly
     * @return
     */
    Set<FeatureKey<?>> satisfies();

    /**
     * The key to use to pull computed feature values out of a record; also reifies the feature value type to
     * expect when pulling the value out (avoids extra casts for api usage convenience)
     * @param <T>
     */
    public class FeatureKey<T> {
        // unique moniker to identify this feature; the key is just a reified version of this key basically with
        // the additional info of what class the feature value is
        private final String key;
        // the value of the feature is an instance of this class
        private final Class<T> featureClass;

        public FeatureKey(String key, Class<T> featureClass) {
            this.key = checkNotNull(key);
            this.featureClass = featureClass;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FeatureKey<?> that = (FeatureKey<?>) o;

            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return "FeatureKey{" +
                    "key='" + key + '\'' +
                    ", featureClass=" + featureClass +
                    '}';
        }
    }
}
