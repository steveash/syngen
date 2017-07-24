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

/**
 * @author Steve Ash
 */
public class InputField {

    private final String input;
    private final NameEntryField entryField;

    public InputField(String input, NameEntryField entryField) {
        this.input = input;
        this.entryField = entryField;
    }

    public String getInput() {
        return input;
    }

    public NameEntryField getEntryField() {
        return entryField;
    }

    @Override
    public String toString() {
        return "InputField{" +
                "input='" + input + '\'' +
                ", entryField=" + entryField +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InputField that = (InputField) o;

        if (input != null ? !input.equals(that.input) : that.input != null) return false;
        return entryField == that.entryField;
    }

    @Override
    public int hashCode() {
        int result = input != null ? input.hashCode() : 0;
        result = 31 * result + (entryField != null ? entryField.hashCode() : 0);
        return result;
    }
}
