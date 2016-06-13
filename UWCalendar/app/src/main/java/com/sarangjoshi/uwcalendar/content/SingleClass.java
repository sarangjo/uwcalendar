package com.sarangjoshi.uwcalendar.content;

import android.content.Intent;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class SingleClass {
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
        s += ". On days " + getDays();
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
                data.getStringExtra("name"),
                data.getStringExtra("location"),
                data.getIntExtra("days", 0),
                data.getStringExtra("start"),
                data.getStringExtra("end")
        );
    }
}
