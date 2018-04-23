package com.sarangjoshi.uwcalendar.models;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to sort by quarter name.
 */

public class QuarterName implements Comparable<QuarterName> {
    public String name;

    public QuarterName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuarterName that = (QuarterName) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static final List<String> QUARTER_ORDER = Arrays.asList("au", "wi", "sp");

    @Override
    public int compareTo(@NonNull QuarterName o) {
        Integer thisYear = Integer.parseInt(this.name.substring(2));
        Integer otherYear = Integer.parseInt(o.name.substring(2));
        if (!otherYear.equals(thisYear)) {
            return thisYear.compareTo(otherYear);
        }
        return QUARTER_ORDER.indexOf(this.name.substring(0, 2)) - QUARTER_ORDER.indexOf(o.name.substring(0, 2));
    }
}
