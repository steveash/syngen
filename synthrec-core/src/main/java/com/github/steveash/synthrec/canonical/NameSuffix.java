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

package com.github.steveash.synthrec.canonical;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

/**
 * The set of name suffixes we parse from names.
 *
 * @author Steve Ash
 */
public enum NameSuffix {

    // generationals -- the ordinal order of these *is* important; the lower the ordinal implies expected age; i.e.
    // a jr is "younger" than a sr because jr's ordinal is higher than sr's
    Sr(true, "sr"),
    Jr(true, "jr"),
    First(true, "1st"),
    Second(true, "ii", "2nd"),
    Third(true, "iii", "3rd"),
    Fourth(true, "iv", "4th"),
    Fifth(true, "v", "5th"),
    Sixth(true, "vi", "6th"),
    Seventh(true, "vii", "7th"),
    Eigth(true, "viii", "8th"),

    // other?
    Esq(false, "esq", "esquire"),
    PhD(false, "phd"),

    // professional
    Jd(false, "jd"),    // lawyer
    Cde(false, "cde"),    // certified diabetic educator
    Dds(false, "dds"),  // dentist
    Dmd(false, "dmd"),  // dentist
    Do(false, "do"),  // osteopathic physician
    Dpm(false, "dpm"),  // podiatrist
    Dvm(false, "dvm"),  // veterinary
    Pa(false, "pa"),
    Md(false, "md"),
    Rn(false, "rn", "aprn"), //registered nurse
    Np(false, "np", "fnp", "anp", "dnp", "agpcnp", "acnp", "agacnp", "pnp", "gnp", "pmhnp", "whnp"), //nurse practitioner

    // military
    Private(false, "pvt", "pvt2", "pfc"),
    Specialist(false, "spc"),
    WarrantOfficer(false, "wo", "wo1", "cw2", "cw3", "cw4", "cw5", "cwo2", "cwo3", "cwo4", "cwo5"),
    Lieutenant(false, "lt", "2lt", "1lt", "ltjg", "lcdr"),
    Captain(false, "cpt", "capt"),
    Commander(false, "cdr"),
    Major(false, "maj"),
    Colonel(false, "col", "ltcol"),
    General(false, "gen", "bgen", "briggen", "gaf", "majgen", "bg", "mg", "ltg", "ltgen", "ga"),
    Corporal(false, "cpl", "lcpl"),
    Sergeant(false, "sgt", "ssgt", "gysgt", "msgt", "smsgt", "cmsgt", "ccm", "cmsaf", "1stsgt", "mgysgt", "sgtmaj", "sgtmajmarcor", "ssg", "sfc", "msg", "1sg", "sgm", "csm", "sma"),
    Airman(false, "amn", "ab", "a1c", "sra", "aa"),
    Seaman(false, "sn", "sa"),
    Fireman(false, "fn", "fa"),
    PettyOfficer(false, "po", "po3", "po2", "po1", "cpo", "scpo", "mcpo", "mcpon", "cmc", "mcpocg"),
    Ensign(false, "ens"),
    Admiral(false, "adm", "rdml", "radm", "vadm", "fadm"),

    // religious
    Sj(false, "sj"),    // society of jesus, clergy
    Ihs(false, "ihs", "ihc"); //Iesus Hominum Salvator


    public final boolean generational;
    public final List<String> normalizedStrings;
    public final String primaryString;

    private NameSuffix(boolean generational, String... normalizedString) {

        this.generational = generational;
        this.primaryString = normalize(normalizedString[0]);
        List<String> strings = Arrays.asList(normalizedString);
        ListIterator<String> iter = strings.listIterator();
        while (iter.hasNext()) {
            String value = iter.next();
            iter.set(normalize(value));
        }
        this.normalizedStrings = strings;
    }

    /**
     * Returns true if *this* generational suffix is "older" than the passed in suffix; e.g.
     * Jr.isOlderThan(Sr) -> false
     * Fifth.isOlderThan(Fourth) -> false
     * Sr.isOlderThan(Third) -> true
     * Sr.isOlderThan(Jr) -> true
     * Sr.isOlderThan(Sr) -> false
     *
     * throws an illegal argument exception if either *this* or *other* suffix are not generational suffixes
     * @param other
     * @return
     */
    public boolean isOlderThan(NameSuffix other) {
        Preconditions.checkState(this.generational, "can't call isOlderThan on suffixes that aren't generational suffixes");
        Preconditions.checkState(other.generational, "can't call isOlderThan on suffixes that aren't generational suffixes");
        return this.ordinal() < other.ordinal();
    }

    private static String normalize(String value) {
        return value.toUpperCase();
    }

    private static final ImmutableMap<String, NameSuffix> suffixValues;
    private static final Set<NameSuffix> ambiguousSuffixValues = ImmutableSet.of(NameSuffix.Fifth);
    private static final ImmutableMap<String, NameSuffix> unambiguousSuffixValues;

    static {
        Builder<String, NameSuffix> builder = ImmutableMap.builder();
        Builder<String, NameSuffix> filteredBuilder = ImmutableMap.builder();

        for (NameSuffix nameSuffix : NameSuffix.values()) {
            for (String normalizedString : nameSuffix.normalizedStrings) {
                builder.put(normalizedString, nameSuffix);

                if (!ambiguousSuffixValues.contains(nameSuffix))
                    filteredBuilder.put(normalizedString, nameSuffix);
            }
        }
        suffixValues = builder.build();
        unambiguousSuffixValues = filteredBuilder.build();
    }

    public static NameSuffix valueOfSuffixString(String value) {
        NameSuffix nameSuffix = suffixValues.get(normalize(value));
        if (nameSuffix == null)
            throw new IllegalStateException("cannot find " + value);

        return nameSuffix;
    }

    public static boolean isUnambiguousKnownSuffix(String value) {
        return unambiguousSuffixValues.containsKey(normalize(value));
    }

    public static boolean isAnyKnownSuffix(String value) {
        return suffixValues.containsKey(normalize(value));
    }

    public static StripResult stripSuffix(String segment) {
        Matcher matcher = suffixPattern.matcher(segment);
        if (!matcher.matches())
            return StripResult.falseResult;

        String newValue = segment.substring(0, matcher.start(1) - 1).trim();
        String suffix = matcher.group(1);
        return new StripResult(true, suffix, newValue);
    }

    public static final Pattern suffixPattern;

    static {
        String patternString = ".*\\S+\\s(" + Joiner.on('|').join(suffixValues.keySet()) + ")\\s*";
        suffixPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }
}
