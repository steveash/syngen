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

package com.github.steveash.synthrec.string;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Way to match strings with either exact matches or regexes; you can build a pattern matcher up
 * and use them to test strings for membership
 * @author Steve Ash
 */
public class PatternMatcher {

    public static class Builder {

        private final Set<String> exacts = Sets.newHashSet();
        private final List<Pattern> patterns = Lists.newArrayList();

        public Builder matchExact(String exactValueToMatch) {
            exacts.add(exactValueToMatch);
            return this;
        }

        public Builder matchRegex(Pattern regexPattern) {
            patterns.add(regexPattern);
            return this;
        }

        public Builder matchRegex(@Nonnull String regex) {
            patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            return this;
        }

        public PatternMatcher build() {
            return new PatternMatcher(ImmutableSet.copyOf(exacts), ImmutableList.copyOf(patterns));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final ImmutableSet<String> matchExact;
    private final ImmutableList<Pattern> matchRegex;

    private PatternMatcher(ImmutableSet<String> matchExact,
            ImmutableList<Pattern> matchRegex
    ) {
        this.matchExact = matchExact;
        this.matchRegex = matchRegex;
    }

    public boolean matches(String candidate) {
        if (matchExact.contains(candidate)) {
            return true;
        }
        for (Pattern pattern : matchRegex) {
            if (pattern.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }
}
