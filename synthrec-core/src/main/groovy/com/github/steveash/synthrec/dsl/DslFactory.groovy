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

package com.github.steveash.synthrec.dsl

import com.google.common.io.CharSource
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * @author Steve Ash
 */
class DslFactory {

    public static void evaluate(CharSource script, String sourceName, Object spec) {
        CompilerConfiguration cc = makeCompilerConfig();
        GroovyShell shell = new GroovyShell(cc);
        def reader = script.openBufferedStream()
        try {
            def ss = (DelegatingScript) shell.parse(reader, sourceName)
            assert ss != null
            ss.setDelegate(spec)
            ss.run()
        } catch (Exception e) {
            throw new RuntimeException("Problem trying to evaluate the DSL " + sourceName, e)
        } finally {
            reader.close()
        }
    }

    private static CompilerConfiguration makeCompilerConfig() {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.addCompilationCustomizers(new ImportCustomizer().addStaticStars(StringUtils.class.getName()))
        compilerConfig.setScriptBaseClass(DelegatingScript.class.getName());
        return compilerConfig;
    }
}