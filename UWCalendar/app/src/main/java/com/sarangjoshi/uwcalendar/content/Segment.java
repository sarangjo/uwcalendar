package com.sarangjoshi.uwcalendar.content;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    List<SingleClass> classes;

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
    @SuppressLint("DefaultLocale")
    public String toString() {
        return String.format("%d:%d to %d:%d, with %d classes", startHr, startMin, endHr, endMin, classes.size());
    }
}
