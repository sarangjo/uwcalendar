package com.sarangjoshi.uwcalendar.content;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.api.services.calendar.model.Event;
import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.GoogleAuthData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the schedule for a single quarter.
 *
 * @author Sarang Joshi
 */
public class Quarter {
    private List<SingleClass> mClassList;
    private List<String> mClassIds;
    private String mName;

    private FirebaseData fb;
    private GoogleAuthData goog;

    public Quarter(String name) {
        this.mName = name;
        this.mClassList = new ArrayList<>();
        this.mClassIds = new ArrayList<>();

        fb = FirebaseData.getInstance();
        goog = GoogleAuthData.getInstance();
    }

    /**
     * Add a new class to this quarter, given its Firebase ID.
     */
    private void addClass(SingleClass c, String id) {
        this.mClassList.add(c);
        this.mClassIds.add(id);
    }

    /**
     * Gets the classes for this quarter.
     */
    public List<SingleClass> getClasses() {
        return Collections.unmodifiableList(mClassList);
    }

    /**
     * Save a new class to this quarter.
     */
    public void saveClass(SingleClass singleClass) throws IOException {
        /*    // Google
            // List the next 10 events from the primary calendar.
            Event event = SingleClass.createGoogleEvent(mName, singleClass);

            String calendarId = "primary";
            event = goog.getService().events().insert(calendarId, event).execute();

            Log.d("Event created:", event.getId());
            singleClass.setGoogleEventId(event.getId());
        */

        // Firebase
        fb.addClass(mName, singleClass);
    }

    /**
     * Remove a class from this quarter.
     */
    public void removeClass(int position) throws IOException {
        SingleClass singleClass = mClassList.get(position);

        // First the Google event
        /*    String id = singleClass.getGoogleEventId();
            goog.getService().events().delete("primary", id).execute();

            Log.d("Event removed:", id);
        */

        // Then the Database event
        fb.removeClass(mName, mClassIds.get(position));

        // Delete locally (TODO: needed?)
        this.mClassList.remove(position);
        this.mClassIds.remove(position);
    }


    /**
     * Get a list of days that represent the day-by-day breakdown of the schedule.
     */
    public List<Day> getWeek() {
        List<Day> week = new ArrayList<>();

        for (int i = 0; i < ScheduleData.DAYS_MAP.length; i++) {
            week.add(new Day());

            for (SingleClass c : this.mClassList) {
                int days = c.getDays();
                if ((days & (1 << i)) == 1) {
                    week.get(i).add(c);
                }
            }
        }

        return week;
    }

    /**
     * Given a DataSnapshot of the quarter, returns a representation of that quarter's schedule.
     */
    public static Quarter valueOf(DataSnapshot snapshot) {
        Quarter q = new Quarter(snapshot.getKey());
        for (DataSnapshot singleClass : snapshot.getChildren()) {
            q.addClass(singleClass.getValue(SingleClass.class), singleClass.getKey());
        }
        return q;
    }

    /**
     * Connect two quarters together.
     */
    public static void connect(Quarter quarter1, Quarter quarter2) {
        if (quarter1 == null || quarter2 == null) {
            return;
        }
        // Go day by day
        List<Day> week1 = quarter1.getWeek();
        List<Day> week2 = quarter2.getWeek();
    }

    /**
     * Represents a single Day in a schedule.
     */
    public static class Day {
        /**
         * A sorted list of the segments making up the day.
         */
        private List<Segment> segments;

        public Day() {
            segments = new ArrayList<>();
            segments.add(Segment.FREE_DAY);
        }

        /**
         * Adds a class to this day, appropriately reorganizing the existing segments in this day.
         */
        public void add(SingleClass c) {
            Segment seg = new Segment(c.getStart(), c.getEnd(), c);

            // Find the first segment that overlaps with seg
            Segment curr = null;
            int i;
            for (i = 0; i < segments.size(); ++i) {
                curr = segments.get(i);
                if (compare(curr.endHr, curr.endMin, seg.startHr, seg.startMin) > 0) {
                    break;
                }
            }

            // Break off the part of curr that is before seg
            if (compare(curr.startHr, curr.startMin, seg.startHr, seg.startMin) != 0) {
                Segment beforeSeg = new Segment(curr.startHr, curr.startMin, seg.startHr, seg.startMin, null);
                beforeSeg.classes.addAll(curr.classes);
                segments.add(i, beforeSeg);

                segments.remove(curr);

                // Restore curr to be whatever is left over
                Segment beginningOfSeg = new Segment(seg.startHr, seg.startMin, curr.endHr, curr.endMin, null);
                beginningOfSeg.classes.addAll(curr.classes);
                segments.add(i + 1, beginningOfSeg);

                i++;
                curr = beginningOfSeg;
            }

            // Overlap all segments that are completely within seg
            while (compare(curr.endHr, curr.endMin, seg.endHr, seg.endMin) <= 0) {
                // Don't actually split any times, just add the new class
                curr.classes.add(c);

                ++i;
                curr = segments.get(i);
            }

            if (compare(curr.startHr, curr.startMin, seg.endHr, seg.endMin) != 0) {
                // What overlaps with seg
                Segment endOfSeg = new Segment(curr.startHr, curr.startMin, seg.endHr, seg.endMin, null);
                endOfSeg.classes.addAll(seg.classes);
                segments.add(i, endOfSeg);

                segments.remove(curr);

                // What doesn't overlap
                Segment afterSeg = new Segment(seg.endHr, seg.endMin, curr.endHr, curr.endMin, null);
                afterSeg.classes.addAll(curr.classes);
                segments.add(i + 1, afterSeg);
            }

            // TODO: Ensure that the segments are always sorted
        }

        public List<Segment> getSegments() {
            return Collections.unmodifiableList(segments);
        }

        public String toString() {
            return segments.toString();
        }
    }

    /**
     * End not inclusive. [start, end)
     */
    public static class Segment implements Comparable<Segment> {
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

    /**
     * Compares two times.
     */
    private static int compare(int hr1, int min1, int hr2, int min2) {
        int tot1 = hr1 * 60 + min1;
        int tot2 = hr2 * 60 + min2;

        return tot1 - tot2;
    }
}
