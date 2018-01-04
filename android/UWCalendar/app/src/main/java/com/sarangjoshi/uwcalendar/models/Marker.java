package com.sarangjoshi.uwcalendar.models;

import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a marker of classes in the day.
 */
public class Marker implements Comparable<Marker> {
    private final int startHr, startMin;
    /**
     * Map from user ID to class name
     */
    private final Map<String, String> classes;
//    public final List<String> classes;

    /**
     * Creates a new Segment.
     */
    private Marker(int startHr, int startMin) {
        // TODO sanitize
        this.startHr = startHr;
        this.startMin = startMin;
        this.classes = new HashMap<>();
    }

    /**
     * Creates a new Segment given its start and end times.
     */
    private Marker(String time) {
        this(Integer.parseInt(time.substring(0, 2)), Integer.parseInt(time.substring(3)));
    }

    private Marker(String time, Map<String, String> classes) {
        this(time);
        this.classes.putAll(classes);
    }

    /**
     * Compares two times.
     */
    private static int compare(int hr1, int min1, int hr2, int min2) {
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
        for (String id : classes.keySet()) {
            s += " " + id + ": " + classes.get(id);
        }
        return s;
    }

    /**
     * Converts from DataSnapshot to a Marker.
     */
    static Marker valueOf(DataSnapshot markerRef) {
        String time = markerRef.child(FirebaseData.TIME_KEY).getValue().toString();

        // Get user id
        //List<String> classes = new ArrayList<>();
        //for (DataSnapshot c : markerRef.child(FirebaseData.CLASSES_KEY).getChildren()) {
        //    classes.add(Integer.parseInt(c.getKey()), c.getValue().toString());
        //}
        Map<String, String> classes = new HashMap<>();
        for (DataSnapshot c : markerRef.child(FirebaseData.CLASSES_KEY).getChildren()) {
            classes.put(c.getKey(), c.getValue().toString());
        }

        return new Marker(time, classes);
    }

    public String toString(FirebaseData fb) {
        String s = String.format(Locale.US, "%02d:%02d", startHr, startMin);
        for (String id : classes.keySet()) {
            s += " " + fb.getUsernameFromId(id) + ": " + classes.get(id);
        }
        return s;
    }
}
