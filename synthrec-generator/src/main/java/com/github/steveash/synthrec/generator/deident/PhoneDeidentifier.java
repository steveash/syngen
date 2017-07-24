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

package com.github.steveash.synthrec.generator.deident;

import java.util.function.ToDoubleFunction;

import javax.annotation.Resource;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.steveash.synthrec.deident.RemapDeidentifier.Remapper;
import com.github.steveash.synthrec.generator.demo.PhoneGenerator;
import com.github.steveash.synthrec.generator.spring.LazyComponent;
import com.github.steveash.synthrec.phone.PhoneParser;
import com.github.steveash.synthrec.phone.PhoneParser.PhoneModel;
import com.github.steveash.synthrec.stat.RandUtil;
import com.github.steveash.synthrec.stat.ThreadLocalRandomGenerator;
import com.github.steveash.synthrec.string.CharReplacer;
import com.github.steveash.synthrec.string.DigitReplacer;
import com.github.steveash.synthrec.string.PatternExpander;
import com.github.steveash.synthrec.string.PatternReducer;
import com.github.steveash.synthrec.string.StringEscaper;
import com.google.common.base.CharMatcher;

/**
 * @author Steve Ash
 */
@LazyComponent
public class PhoneDeidentifier implements Remapper<String> {

    @Resource private PhoneGenerator phoneGenerator;

    private final PhoneParser parser = new PhoneParser();
    private final StringEscaper escaper = new StringEscaper("ext");
    private final PatternExpander expander = new PatternExpander();
    private final ThreadLocalRandomGenerator rand = RandUtil.threadLocalRand();

    @Override
    public String remap(String input, ToDoubleFunction<String> countForVocab) {
        if (input.length() <= 4) {
            return null; // just leave as is, 4 digits aren't identifying
        }
        RandomGenerator localGen = rand.getThreadGenerator();
        PhoneModel model = parser.parse(input);
        if (model == null || model.getAreaCode().length() != 3) {
            return naiveRemap(localGen, input);
        }
        // lets gen a realistic phone number with this area code
        String phone10 = phoneGenerator.generateGivenArea(localGen, model.getAreaCode());
        input = CharMatcher.digit().replaceFrom(input, '9');
        input = DigitReplacer.replacePatternLtoR(localGen, input, phone10);
        input = CharReplacer.replaceConsecutiveAlpha(localGen, input, 4);
        return input;
    }

    private String naiveRemap(RandomGenerator localGen, String input) {
        input = escaper.escape(input);
        String pattern = PatternReducer.replace(input);
        return expander.expand(localGen, pattern);
    }
}
