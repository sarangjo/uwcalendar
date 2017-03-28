package com.sarangjoshi.uwcalendar.content;

import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sarangjoshi.uwcalendar.singletons.FirebaseData.CLASSES_KEY;

/**
 * Represents the schedule for a single quarter.
 *
 * @author Sarang Joshi
 */
public class Quarter {
    private static final String USER_ID_KEY = "userId";
    public static final String QUARTER_NAME_KEY = "quarterName";
    private static final String CLASS_IDS_KEY = "classIds";

    private ArrayList<SingleClass> mClassList;
    private ArrayList<String> mClassIds;
    private String mName;
    private String mUserId;

    private FirebaseData fb;

    public Quarter(String userId, String quarterName) {
        this.mName = quarterName;
        this.mUserId = userId;
        this.mClassList = new ArrayList<>();
        this.mClassIds = new ArrayList<>();

        fb = FirebaseData.getInstance();
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
    static Quarter valueOf(String id, DataSnapshot snapshot) {
        Quarter q = new Quarter(id, snapshot.getKey());
        for (DataSnapshot singleClass : snapshot.getChildren()) {
            q.addClass(singleClass.getValue(SingleClass.class), singleClass.getKey());
        }
        return q;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        ArrayList<String> classStrings = new ArrayList<>();
        for (SingleClass c : mClassList) {
            classStrings.add(c.serialize());
        }
        bundle.putString(USER_ID_KEY, mUserId);
        bundle.putString(QUARTER_NAME_KEY, mName);
        bundle.putStringArrayList(CLASSES_KEY, classStrings);
        bundle.putStringArrayList(CLASS_IDS_KEY, mClassIds);
        return bundle;
    }

    public static Quarter valueOf(Bundle bundle) {
        Quarter q = new Quarter(bundle.getString(USER_ID_KEY), bundle.getString(QUARTER_NAME_KEY));
        List<String> classStrings = bundle.getStringArrayList(CLASSES_KEY);
        List<String> classIds = bundle.getStringArrayList(CLASS_IDS_KEY);
        for (int i = 0; i < classStrings.size(); i++) {
            q.addClass(SingleClass.deserialize(classStrings.get(i)), classIds.get(i));
        }
        return q;
    }
}
