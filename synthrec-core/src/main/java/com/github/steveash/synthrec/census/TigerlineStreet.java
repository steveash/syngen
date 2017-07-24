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

/**
 * @author Steve Ash
 */
public class TigerlineStreet {

    private long TLID;
    private String FULLNAME;
    private String NAME;
    private String PREDIRABRV;
    private String PRETYPABRV;
    private String PREQUALABR;
    private String SUFDIRABRV;
    private String SUFTYPABRV;
    private String SUFQUALABR;
    private String PREDIR;
    private String PRETYP;
    private String PREQUAL;
    private String SUFDIR;
    private String SUFTYP;
    private String SUFQUAL;
    private String LINEARID;
    private String MTFCC;
    private String PAFLAG;

    public long getTLID() {
        return TLID;
    }

    public void setTLID(long TLID) {
        this.TLID = TLID;
    }

    public String getFULLNAME() {
        return FULLNAME;
    }

    public void setFULLNAME(String FULLNAME) {
        this.FULLNAME = FULLNAME;
    }

    public String getNAME() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME = NAME;
    }

    public String getPREDIRABRV() {
        return PREDIRABRV;
    }

    public void setPREDIRABRV(String PREDIRABRV) {
        this.PREDIRABRV = PREDIRABRV;
    }

    public String getPRETYPABRV() {
        return PRETYPABRV;
    }

    public void setPRETYPABRV(String PRETYPABRV) {
        this.PRETYPABRV = PRETYPABRV;
    }

    public String getPREQUALABR() {
        return PREQUALABR;
    }

    public void setPREQUALABR(String PREQUALABR) {
        this.PREQUALABR = PREQUALABR;
    }

    public String getSUFDIRABRV() {
        return SUFDIRABRV;
    }

    public void setSUFDIRABRV(String SUFDIRABRV) {
        this.SUFDIRABRV = SUFDIRABRV;
    }

    public String getSUFTYPABRV() {
        return SUFTYPABRV;
    }

    public void setSUFTYPABRV(String SUFTYPABRV) {
        this.SUFTYPABRV = SUFTYPABRV;
    }

    public String getSUFQUALABR() {
        return SUFQUALABR;
    }

    public void setSUFQUALABR(String SUFQUALABR) {
        this.SUFQUALABR = SUFQUALABR;
    }

    public String getPREDIR() {
        return PREDIR;
    }

    public void setPREDIR(String PREDIR) {
        this.PREDIR = PREDIR;
    }

    public String getPRETYP() {
        return PRETYP;
    }

    public void setPRETYP(String PRETYP) {
        this.PRETYP = PRETYP;
    }

    public String getPREQUAL() {
        return PREQUAL;
    }

    public void setPREQUAL(String PREQUAL) {
        this.PREQUAL = PREQUAL;
    }

    public String getSUFDIR() {
        return SUFDIR;
    }

    public void setSUFDIR(String SUFDIR) {
        this.SUFDIR = SUFDIR;
    }

    public String getSUFTYP() {
        return SUFTYP;
    }

    public void setSUFTYP(String SUFTYP) {
        this.SUFTYP = SUFTYP;
    }

    public String getSUFQUAL() {
        return SUFQUAL;
    }

    public void setSUFQUAL(String SUFQUAL) {
        this.SUFQUAL = SUFQUAL;
    }

    public String getLINEARID() {
        return LINEARID;
    }

    public void setLINEARID(String LINEARID) {
        this.LINEARID = LINEARID;
    }

    public String getMTFCC() {
        return MTFCC;
    }

    public void setMTFCC(String MTFCC) {
        this.MTFCC = MTFCC;
    }

    public String getPAFLAG() {
        return PAFLAG;
    }

    public void setPAFLAG(String PAFLAG) {
        this.PAFLAG = PAFLAG;
    }

    @Override
    public String toString() {
        return "TigerlineStreet{" +
                "TLID=" + TLID +
                ", FULLNAME='" + FULLNAME + '\'' +
                ", NAME='" + NAME + '\'' +
                ", PREDIRABRV='" + PREDIRABRV + '\'' +
                ", PRETYPABRV='" + PRETYPABRV + '\'' +
                ", PREQUALABR='" + PREQUALABR + '\'' +
                ", SUFDIRABRV='" + SUFDIRABRV + '\'' +
                ", SUFTYPABRV='" + SUFTYPABRV + '\'' +
                ", SUFQUALABR='" + SUFQUALABR + '\'' +
                ", PREDIR='" + PREDIR + '\'' +
                ", PRETYP='" + PRETYP + '\'' +
                ", PREQUAL='" + PREQUAL + '\'' +
                ", SUFDIR='" + SUFDIR + '\'' +
                ", SUFTYP='" + SUFTYP + '\'' +
                ", SUFQUAL='" + SUFQUAL + '\'' +
                ", LINEARID='" + LINEARID + '\'' +
                ", MTFCC='" + MTFCC + '\'' +
                ", PAFLAG='" + PAFLAG + '\'' +
                '}';
    }
}
