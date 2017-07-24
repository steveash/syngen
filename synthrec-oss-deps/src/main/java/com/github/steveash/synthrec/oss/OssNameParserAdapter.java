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

package com.github.steveash.synthrec.oss;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.steveash.synthrec.name.InputField;
import com.github.steveash.synthrec.name.NameEntryField;
import com.github.steveash.synthrec.name.NamePart;
import com.github.steveash.synthrec.name.NameTagger;
import com.github.steveash.synthrec.name.NameToken;
import com.github.steveash.synthrec.name.Names;
import com.github.steveash.synthrec.name.PersonalName;
import com.github.steveash.synthrec.phonetic.PhoneEncoder;
import com.google.common.collect.Lists;
import com.tupilabs.human_name_parser.HumanNameParser;
import com.tupilabs.human_name_parser.Label;
import com.tupilabs.human_name_parser.ParsedName;

/**
 * This name parser (HumanNameParser library) that I forked is really bad. Please don't use this for real -- find your
 * own or use SAS Dataflux
 * @author Steve Ash
 */
public class OssNameParserAdapter implements NameTagger {

    private static final Pattern cleaner = Pattern.compile("(^[\\W]*)|([^.\\w]*$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern abbrev = Pattern.compile("\\W", Pattern.CASE_INSENSITIVE);

    private final PhoneEncoder phoneEncoder;
    private final HumanNameParser parser = new HumanNameParser(true);

    public OssNameParserAdapter(PhoneEncoder phoneEncoder) {
        this.phoneEncoder = phoneEncoder;
    }

    @Override
    public PersonalName parse(List<InputField> fields) {
        // unfortunately the open source parser can't use any heuristics about the incoming segment
        String joined = fields.stream().map(InputField::getInput).collect(Collectors.joining(" "));
        ParsedName name = parser.parse(joined);
        List<NameToken> tokens = Lists.newArrayList();
        for (int i = 0; i < name.size(); i++) {
            String token = name.getToken(i);
            Label label = name.getLabel(i);
            append(tokens, fields, token, label);
        }
        return PersonalName.make(joined, tokens, 1.0);
    }

    private void append(List<NameToken> tokens, List<InputField> fields, String token, Label label) {
        if (isNotBlank(token) || label == Label.Whitespace) {
            NamePart namePart = translateLabel(label);
            String dictNormal = Names.normalize(token);
            tokens.add(NameToken.make(
                    token,
                    cleanToken(token),
                    abbrev(token),
                    dictNormal,
                    phoneEncoder.encodeAllGreaterThan(dictNormal, Names.MIN_PHONETIC_KEY),
                    findEntryFrom(token, fields)
                    ).withPart(namePart)

            );
        }
    }

    private NamePart translateLabel(Label label) {
        switch (label) {
            case First:
                return NamePart.GivenName;
            case FirstInitial:
                return NamePart.FirstInitial;
            case Last:
                return NamePart.Surname;
            case Middle:
                return NamePart.MiddleName;
            case MiddleInital:
                return NamePart.MiddleInitial;
            case Nickname:
                return NamePart.Nickname;
            case Postnominal:
                return NamePart.SuffixOther;
            case Salutation:
                return NamePart.Prefix;
            case Suffix:
                return NamePart.SuffixGenerational;
            case Whitespace:
                throw new IllegalArgumentException(); // should've already skipped
            case Unknown:
                return NamePart.Unknown;
            default:
                throw new IllegalArgumentException("Dont know how to handle Human Name Parser (OSS) label " +
                        label);
        }
    }

    private NameEntryField findEntryFrom(String token, List<InputField> fields) {
        for (InputField field : fields) {
            if (field.getInput().toLowerCase().contains(token.toLowerCase())) {
                return field.getEntryField();
            }
        }
        return NameEntryField.Unknown;
    }

    String cleanToken(String original) {
        Matcher matcher = cleaner.matcher(original.toLowerCase());
        return matcher.replaceAll("");
    }

    String abbrev(String cleaned) {
        return abbrev.matcher(cleaned).replaceAll("");
    }
}
