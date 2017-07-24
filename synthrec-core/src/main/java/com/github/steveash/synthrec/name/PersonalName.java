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

package com.github.steveash.synthrec.name;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Domain object representing a whole name. It is made up of tokens and they might be
 * labeled
 * @author Steve Ash
 */
public class PersonalName {

    public static final PersonalName NULL_NAME = make("", ImmutableList.of(), 0);

    private static final Joiner spaceJoin = Joiner.on(' ');

    public static PersonalName make(String input, List<NameToken> tokens, double score) {
        return new PersonalName(input, tokens, score, null, 0, null, null);
    }

    private final String input;
    private final List<NameToken> tokens;
    private final double score;         // score from the CRF
    private final String culture;
    private final String givenNameCulture;
    private final String familyNameCulture;
    private final double rerankScore;   // score from the reranker

    private PersonalName(
            String input,
            List<NameToken> tokens,
            double score,
            String culture,
            double rerankScore,
            String givenNameCulture,
            String familyNameCulture
    ) {
        this.input = input;
        this.tokens = tokens;
        this.score = score;
        this.culture = culture;
        this.rerankScore = rerankScore;
        this.givenNameCulture = givenNameCulture;
        this.familyNameCulture = familyNameCulture;
    }

    public String getInput() {
        return input;
    }

    public List<NameToken> getTokens() {
        return tokens;
    }

    public Stream<NameToken> filterTokensTagged(NamePart... nameParts) {
        HashSet<NamePart> acceptable = Sets.newHashSet(nameParts);
        return tokens.stream().filter(np -> acceptable.contains(np.getPart()));
    }

    public NameToken get(int i) {
        return tokens.get(i);
    }

    public int size() {
        return tokens.size();
    }

    public boolean labelsMatchTo(PersonalName gold) {
        Preconditions.checkArgument(gold.getTokens().size() == this.tokens.size());
        for (int i = 0; i < gold.getTokens().size(); i++) {
            if (gold.getTokens().get(i).getPart() != tokens.get(i).getPart()) {
                return false;
            }
        }
        return true;
    }

    public double getScore() {
        return score;
    }

    public double getRerankScore() {
        return rerankScore;
    }

    public String getCulture() {
        return culture;
    }

    public String getGivenNameCulture() {
        return givenNameCulture;
    }

    public String getFamilyNameCulture() {
        return familyNameCulture;
    }

    public String labelString() {
        return tokens.stream().map(NameToken::getPart).map(NamePart::name).collect(Collectors.joining(","));
    }

    public PersonalName withCulture(String newCulture) {
        return new PersonalName(this.input, this.tokens, this.score, newCulture, rerankScore, this.givenNameCulture, this.familyNameCulture);
    }

    public PersonalName withCulture(String newCulture, String givenNameCulture, String familyNameCulture) {
        return new PersonalName(this.input, this.tokens, this.score, newCulture, rerankScore, givenNameCulture, familyNameCulture);
    }

    public PersonalName withRerankScore(double newRrScore) {
        return new PersonalName(this.input, this.tokens, this.score, culture, newRrScore, this.givenNameCulture, this.familyNameCulture);
    }

    public PersonalName withReplacedTokens(@Nullable Int2ObjectArrayMap<NameToken> updates) {
        if (updates == null || updates.isEmpty()) {
            return this;
        }
        ArrayList<NameToken> replaced = Lists.newArrayList(this.tokens);
        ObjectIterator<Entry<NameToken>> iter = updates.int2ObjectEntrySet().fastIterator();
        while (iter.hasNext()) {
            Entry<NameToken> next = iter.next();
            replaced.set(next.getIntKey(), next.getValue());
        }
        return withNameTokens(replaced);
    }

    public PersonalName withNameTokens(List<NameToken> newTokens) {
        return new PersonalName(this.input,
                newTokens,
                this.score,
                this.culture,
                this.rerankScore,
                this.givenNameCulture,
                this.familyNameCulture
        );
    }

    @Override
    public String toString() {
        return "PersonalName{" +
                "input='" + input + '\'' +
                ", tokens=" + labelString() +
                ", culture=" + culture +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PersonalName that = (PersonalName) o;

        return input.equals(that.input);
    }

    @Override
    public int hashCode() {
        return input.hashCode();
    }
}
