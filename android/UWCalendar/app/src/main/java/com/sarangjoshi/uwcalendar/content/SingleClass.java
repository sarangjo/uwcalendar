package com.sarangjoshi.uwcalendar.content;

import android.content.Intent;
import android.text.TextUtils;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.activities.AddClassActivity;
import com.sarangjoshi.uwcalendar.activities.SaveToGoogleActivity;
import com.sarangjoshi.uwcalendar.singletons.ScheduleData;

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

    private String id;

    private String googleEventId;

    public SingleClass() {
        // Do nothing
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
     * Manufacture a Google event from the given class details.
     */
    public Event createGoogleEvent(String quarter) {
        Event event = new Event().setSummary(getName())
                .setLocation(getLocation())
                .setId(getGoogleEventId());

        String[] qtrDetails = ScheduleData.getInstance().getQuarterInfo(quarter);

        // RECURRENCE DETAILS
        int[] offset = new int[1];
        String days = getDaysString(offset);
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
            // TODO higgety handle this
        }
        c.add(Calendar.DATE, offset[0]);

        // TODO: what to do for timezone
        String startString = sdf.format(c.getTime()) + "T" + getStart() + ":00-07:00";
        DateTime startDateTime = new DateTime(startString);
        EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("America/Los_Angeles");
        event.setStart(start);

        String endString = startString.substring(0, 11) + getEnd() + startString.substring(16);
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

    String serialize() {
        String[] arr = {name, location, "" + days, start, end, id, googleEventId};
        return TextUtils.join(";;", arr);
    }

    static SingleClass deserialize(String s) {
        String[] arr = s.split(";;");
        SingleClass c = new SingleClass(arr[0], arr[1], Integer.parseInt(arr[2]), arr[3], arr[4]);
        c.setId(arr[5].equals("null") ? null : arr[5]);
        c.setGoogleEventId(arr[6].equals("null") ? null : arr[6]);
        return c;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Initializes a new SingleClass from the given Intent.
     */
    public static SingleClass valueOf(Intent data) {
        SingleClass c = new SingleClass(
                data.getStringExtra(AddClassActivity.NAME_KEY),
                data.getStringExtra(AddClassActivity.LOCATION_KEY),
                data.getIntExtra(AddClassActivity.DAYS_KEY, 0),
                data.getStringExtra(AddClassActivity.START_KEY),
                data.getStringExtra(AddClassActivity.END_KEY)
        );
        c.setId(data.getStringExtra(ScheduleData.CLASS_ID_KEY));
        c.setGoogleEventId(data.getStringExtra(SaveToGoogleActivity.GOOGLE_EVENT_ID_KEY));
        return c;
    }

    /**
     * Initializes a new SingleClass from the given DataSnapshot.
     */
    public static SingleClass valueOf(DataSnapshot snapshot) {
        SingleClass c = snapshot.getValue(SingleClass.class);

        // Set firebase ID on download
        c.setId(snapshot.getKey());

        return c;
    }
}
