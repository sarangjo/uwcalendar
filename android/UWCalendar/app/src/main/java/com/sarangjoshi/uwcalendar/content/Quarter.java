package com.sarangjoshi.uwcalendar.content;

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
    private String mId;

    private FirebaseData fb;
    private GoogleAuthData goog;

    public Quarter(String userId, String name) {
        this.mName = name;
        this.mId = userId;
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
     * Given a DataSnapshot of the quarter, returns a representation of that quarter's schedule.
     */
    public static Quarter valueOf(String id, DataSnapshot snapshot) {
        Quarter q = new Quarter(id, snapshot.getKey());
        for (DataSnapshot singleClass : snapshot.getChildren()) {
            q.addClass(singleClass.getValue(SingleClass.class), singleClass.getKey());
        }
        return q;
    }

}
