package com.sarangjoshi.uwcalendar.content;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.GoogleAuthData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
     * Remove a class from this quarter.
     */
    public void removeClass(int position) throws IOException {
        SingleClass singleClass = mClassList.get(position);

        // First the Google event
        goog.getService().events().delete("primary", singleClass.getGoogleEventId()).execute();

        // Then the Database event
        fb.removeClass(mName, mClassIds.get(position));

        // Delete locally (TODO: needed?)
        this.mClassList.remove(position);
        this.mClassIds.remove(position);
    }

    /**
     * Save a new class to this quarter.
     */
    public void saveClass(SingleClass singleClass) throws IOException {
        // Google
        // List the next 10 events from the primary calendar.
        Event event = createGoogleEvent(mName, singleClass);

        String calendarId = "primary";
        event = goog.getService().events().insert(calendarId, event).execute();

        Log.d("Event created:", event.getId());
        singleClass.setGoogleEventId(event.getId());

        // Firebase
        fb.addClass(mName, singleClass);
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
     * Manufacture a Google event from the given class details.
     */
    public static Event createGoogleEvent(String quarter, SingleClass singleClass) {
        Event event = new Event().setSummary(singleClass.getName())
                .setLocation(singleClass.getLocation());

        String[] qtrDetails = ScheduleData.getInstance().getQuarterInfo(quarter);

        // RECURRENCE DETAILS
        String[] recurrenceDays = {"MO", "TU", "WE", "TH", "FR"};
        String days = "";
        int offset = 0;
        for (int i = 0; i < recurrenceDays.length; i++) {
            if ((singleClass.getDays() & (1 << i)) != 0) {
                if (days.isEmpty()) {
                    offset = i;
                } else {
                    days += ",";
                }
                days += recurrenceDays[i];
            }
        }
        String endDate = (qtrDetails[1]).replaceAll("-", "");
        String[] recurrence = new String[]{"RRULE:FREQ=WEEKLY;UNTIL=" + endDate + "T115959Z;WKST=SU;BYDAY=" + days};
        event.setRecurrence(Arrays.asList(recurrence));

        // Expand start/end time to include full date
        String monday = qtrDetails[0];  // Start date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar c = Calendar.getInstance();
        try {
            c.setTime(sdf.parse(monday)); // TODO: timezone?
        } catch (ParseException ignored) {
        }
        c.add(Calendar.DATE, offset);

        String startString = sdf.format(c.getTime()) + "T" + singleClass.getStart() + ":00-07:00";
        DateTime startDateTime = new DateTime(startString);
        EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("America/Los_Angeles");
        event.setStart(start);

        String endString = startString.substring(0, 11) + singleClass.getEnd() + startString.substring(16);
        DateTime endDateTime = new DateTime(endString);
        EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("America/Los_Angeles");
        event.setEnd(end);

        return event;
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

        // Merge the two weeks
        // TODO
    }

    /**
     * Represents a single Day in a schedule.
     */
    private static class Day {
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

            // Find the segment that contains this timeslot
            Segment curr = null;
            for (int i = 0; i < segments.size(); ++i) {
                curr = segments.get(i);
                if (curr.singleClass == null && curr.endHr >= seg.endHr && curr.endMin >= seg.endMin) {
                    break;
                }
            }
            // If we reached here, it's to be replaced with the last element
            segments.remove(curr);
            Segment beforeSeg = new Segment(curr.startHr, curr.startMin, seg.startHr, seg.startMin, null);
            segments.add(beforeSeg);
            segments.add(seg);
            if (seg.endHr != curr.endHr || seg.endMin != curr.endMin) {
                Segment afterSeg = new Segment(seg.endHr, seg.endMin, curr.endHr, curr.endMin, null);
                segments.add(afterSeg);
            }

            Collections.sort(segments);
        }

        public String toString() {
            return segments.toString();
        }

    }

    /**
     * End not inclusive. [start, end)
     */
    private static class Segment implements Comparable<Segment> {
        public static final Segment FREE_DAY = new Segment(0, 0, 24, 0, null);

        int startHr, startMin;
        int endHr, endMin;
        /**
         * If singleClass == null, this segment is free in the schedule
         */
        SingleClass singleClass;

        public Segment(int startHr, int startMin, int endHr, int endMin, SingleClass singleClass) {
            this.startHr = startHr;
            this.startMin = startMin;
            this.endHr = endHr;
            this.endMin = endMin;
            this.singleClass = singleClass;
        }

        /**
         * Creates a new Segment given its start and end times.
         */
        public Segment(String start, String end, SingleClass c) {
            String[] times = start.split(":");
            startHr = Integer.parseInt(times[0]);
            startMin = Integer.parseInt(times[1]);

            String[] endTimes = end.split(":");
            endHr = Integer.parseInt(endTimes[0]);
            endMin = Integer.parseInt(endTimes[1]);

            singleClass = c;
        }

        @Override
        public int compareTo(Segment another) {
            if (startHr == another.startHr) {
                if (startMin == another.startMin) {
                    if (endHr == another.endHr) {
                        return endMin - another.endMin;
                    }
                    return endHr - another.endHr;
                }
                return startMin - another.startMin;
            }
            return startHr - another.startHr;
        }

        /**
         * Returns a String representation of this Segment.
         */
        public String toString() {
            return String.format("%d:%d to %d:%d", startHr, startMin, endHr, endMin);
        }
    }

}
