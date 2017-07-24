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

package com.github.steveash.synthrec.census;

import java.io.File;
import java.util.List;

import org.jamel.dbf.processor.DbfProcessor;
import org.jamel.dbf.utils.DbfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.synthrec.canonical.Normalizers;
import com.google.common.base.Charsets;

/**
 * @author Steve Ash
 */
public class TigerlineStreetReader {
    private static final Logger log = LoggerFactory.getLogger(TigerlineStreetReader.class);

    public static List<TigerlineStreet> readStreets(File input) {
        return DbfProcessor.loadData(input, row -> {
            TigerlineStreet street = new TigerlineStreet();
            street.setTLID(((Double)row[0]).longValue());
            street.setFULLNAME(makeString(row[1]));
            street.setNAME(makeString(row[2]));
            street.setPREDIRABRV(makeString(row[3]));
            street.setPRETYPABRV(makeString(row[4]));
            street.setPREQUALABR(makeString(row[5]));
            street.setSUFDIRABRV(makeString(row[6]));
            street.setSUFTYPABRV(makeString(row[7]));
            street.setSUFQUALABR(makeString(row[8]));
            street.setPREDIR(makeString(row[9]));
            street.setPRETYP(makeString(row[10]));
            street.setPREQUAL(makeString(row[11]));
            street.setSUFDIR(makeString(row[12]));
            street.setSUFTYP(makeString(row[13]));
            street.setSUFQUAL(makeString(row[14]));
            street.setLINEARID(makeString(row[15]));
            street.setMTFCC(makeString(row[16]));
            street.setPAFLAG(makeString(row[17]));
            return street;
        });
    }

    private static String makeString(Object bytes) {
        return Normalizers.interner().intern(new String((byte[]) bytes, Charsets.UTF_8).trim());
    }
}
