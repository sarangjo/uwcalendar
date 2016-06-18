package com.sarangjoshi.uwcalendar.content;

import android.content.Intent;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.sarangjoshi.uwcalendar.AddClassActivity;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class SingleClass {
    public static final String[] RECURRENCE_DAYS = {"MO", "TU", "WE", "TH", "FR"};

    private String name;
    private String location;
    private int days;
    private String start;
    private String end;

    private String googleEventId;

    public SingleClass() {
    }

    public SingleClass(String name, String location, int days, String start, String end) {
        this.name = name;
        this.location = location;
        this.days = days;
        this.start = start;
        this.end = end;
    }

    private static Map<String, SingleClass> internMap = new HashMap<>();

    /**
     * Creates a class with the given name. Interned.
     */
    public static SingleClass createClass(String name) {
        if (!internMap.containsKey(name)) {
            SingleClass c = new SingleClass();
            c.name = name;
            internMap.put(name, c);
        }
        return internMap.get(name);
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public int getDays() {
        return days;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String toString() {
        String s = getName() + ", " + getLocation() + ". From " + getStart() + " to " + getEnd();
        s += ". On days " + getDaysString(null);
        return s;
    }

    public String getGoogleEventId() {
        return googleEventId;
    }

    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }

    /**
     * Initializes a new SingleClass from the given Intent.
     *
     * @param data
     * @return
     */
    public static SingleClass valueOf(Intent data) {
        return new SingleClass(
                data.getStringExtra(AddClassActivity.NAME_KEY),
                data.getStringExtra(AddClassActivity.LOCATION_KEY),
                data.getIntExtra(AddClassActivity.DAYS_KEY, 0),
                data.getStringExtra(AddClassActivity.START_KEY),
                data.getStringExtra(AddClassActivity.END_KEY)
        );
    }

    /**
     * Manufacture a Google event from the given class details.
     */
    public static Event createGoogleEvent(String quarter, SingleClass singleClass) {
        Event event = new Event().setSummary(singleClass.getName())
                .setLocation(singleClass.getLocation());

        String[] qtrDetails = ScheduleData.getInstance().getQuarterInfo(quarter);

        // RECURRENCE DETAILS
        int[] offset = new int[1];
        String days = singleClass.getDaysString(offset);
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
        c.add(Calendar.DATE, offset[0]);

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
     * TODO
     *
     * @param offset a 1-element array to pass out the offset for this class
     */
    private String getDaysString(int[] offset) {
        String days = "";
        int val = 0;
        for (int i = 0; i < RECURRENCE_DAYS.length; i++) {
            if ((getDays() & (1 << i)) != 0) {
                if (days.isEmpty()) {
                    val = i;
                } else {
                    days += ",";
                }
                days += RECURRENCE_DAYS[i];
            }
        }
        if (offset != null && offset.length == 1) offset[0] = val;
        return days;
    }
}
