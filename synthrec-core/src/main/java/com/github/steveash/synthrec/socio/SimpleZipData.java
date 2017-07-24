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

package com.github.steveash.synthrec.socio;

/**
 * @author Steve Ash
 */
public class SimpleZipData implements ZipData {

    private final String zipcode;
    private final int population;
    private final String city;
    private final String state;
    private final String fips;
    private final int fipsPopulation;
    private final String areaCode;

    public SimpleZipData(String zipcode,
            int population,
            String city,
            String state,
            String fips,
            int fipsPopulation,
            String areaCode
    ) {
        this.zipcode = zipcode;
        this.population = population;
        this.city = city;
        this.state = state;
        this.fips = fips;
        this.fipsPopulation = fipsPopulation;
        this.areaCode = areaCode;
    }

    @Override
    public String getZipcode() {
        return zipcode;
    }

    @Override
    public int getEstimatedPopulation() {
        return population;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getFips() {
        return fips;
    }

    @Override
    public int getFipsPopulation() {
        return fipsPopulation;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getAreaCode() {
        return areaCode;
    }
}
