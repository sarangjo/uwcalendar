package com.sarangjoshi.uwcalendar.content;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.data.FirebaseData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TODO
 */
public class Marker implements Comparable<Marker> {
    public final int startHr, startMin;
    public final List<String> classes;

    /**
     * Creates a new Segment.
     */
    public Marker(int startHr, int startMin) {
        // TODO sanitize
        this.startHr = startHr;
        this.startMin = startMin;
        this.classes = new ArrayList<>();
    }

    /**
     * Creates a new Segment given its start and end times.
     */
    public Marker(String time) {
        this(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3)));
    }

    public Marker(String time, List<String> classes) {
        this(time);
        for (String c : classes) {
            this.classes.add(c);
        }
    }

    /**
     * Compares two times.
     */
    public static int compare(int hr1, int min1, int hr2, int min2) {
        int tot1 = hr1 * 60 + min1;
        int tot2 = hr2 * 60 + min2;

        return tot1 - tot2;
    }

    @Override
    public int compareTo(@NonNull Marker another) {
        return compare(startHr, startMin, another.startHr, another.startMin);
    }

    /**
     * Returns a String representation of this Segment.
     */
    public String toString() {
        String s = String.format(Locale.US, "%02d:%02d", startHr, startMin);
        for (int i = 0; i < classes.size(); i++) {
            s += " " + i + ": " + classes.get(i);
        }
        return s;
    }

    /**
     * Converts from DataSnapshot to a Marker.
     */
    public static Marker valueOf(DataSnapshot markerRef) {
        String time = markerRef.child(FirebaseData.TIME_KEY).getValue().toString();

        // Get user id
        List<String> classes = new ArrayList<>();
        for (DataSnapshot c : markerRef.child(FirebaseData.CLASSES_KEY).getChildren()) {
            classes.add(Integer.parseInt(c.getKey()), c.getValue().toString());
        }

        return new Marker(time, classes);
    }
}
