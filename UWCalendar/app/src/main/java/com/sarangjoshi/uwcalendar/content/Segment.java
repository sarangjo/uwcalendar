package com.sarangjoshi.uwcalendar.content;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.sarangjoshi.uwcalendar.data.FirebaseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * End not inclusive. [start, end)
 */
public class Segment implements Comparable<Segment> {
    public static final Segment FREE_DAY = new Segment(0, 0, 24, 0, null);

    public final int startHr, startMin;
    public final int endHr, endMin;

    /**
     * If classes is empty, this segment is free in the schedule
     */
    public List<SingleClass> classes;

    /**
     * A map from user ID to class.
     */
    public Map<String, SingleClass> classesMap;

    /**
     * Creates a new Segment.
     */
    public Segment(int startHr, int startMin, int endHr, int endMin, SingleClass c) {
        this.startHr = startHr;
        this.startMin = startMin;
        this.endHr = endHr;
        this.endMin = endMin;
        this.classes = new ArrayList<>();
        if (c != null)
            this.classes.add(c);
    }

    /**
     * Creates a new Segment given its start and end times.
     */
    public Segment(String start, String end, SingleClass c) {
        String[] times = start.split(":");
        String[] endTimes = end.split(":");

        this.startHr = Integer.parseInt(times[0]);
        this.startMin = Integer.parseInt(times[1]);
        this.endHr = Integer.parseInt(endTimes[0]);
        this.endMin = Integer.parseInt(endTimes[1]);

        this.classes = new ArrayList<>();
        if (c != null)
            this.classes.add(c);
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
    public int compareTo(@NonNull Segment another) {
        int start = compare(startHr, startMin, another.startHr, another.startMin);
        if (start == 0) {
            return compare(endHr, endMin, another.endHr, another.endMin);
        }
        return start;
    }

    public List<SingleClass> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    /**
     * Returns a String representation of this Segment.
     */
    public String toString() {
        String s = String.format(Locale.US, "%02d:%02d to %02d:%02d.", startHr, startMin, endHr, endMin);
        for (SingleClass c : classes) {
            s += " " + c.getName();
        }
        return s;
    }

    /**
     * Given a snapshot of a segment reference in a connection, creates a Segment that is the value
     * of the given reference.
     */
    public static Segment valueOf(DataSnapshot segRef) {
        String start = segRef.child(FirebaseData.START_KEY).getValue().toString();
        String end = segRef.child(FirebaseData.END_KEY).getValue().toString();
        List<SingleClass> classes = new ArrayList<>();
        for (DataSnapshot c : segRef.child(FirebaseData.CLASSES_KEY).getChildren()) {
            classes.add(SingleClass.createClass(c.getValue().toString()));
        }
        Segment s = new Segment(start, end, null);
        s.classes.addAll(classes);

        return s;
    }
}
