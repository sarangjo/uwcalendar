package com.sarangjoshi.uwcalendar.content;

import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;

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
    public static final String QUARTER_ID_KEY = "quarterId";

    private ArrayList<SingleClass> mClassList;
    private String mId;
    private String mUserId;

    public Quarter(String userId, String quarterId) {
        this.mId = quarterId;
        this.mUserId = userId;
        this.mClassList = new ArrayList<>();
    }

    public Quarter(String userId, String quarterId, List<SingleClass> classes) {
        this(userId, quarterId);

        mClassList.addAll(classes);
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
//    public void removeClass(int position) throws IOException {
//        // First the Google event
//        /*    String id = singleClass.getGoogleEventId();
//            goog.getService().events().delete("primary", id).execute();
//
//            Log.d("Event removed:", id);
//        */
//
//        // Then the Database event
//        fb.removeClass(mId, mClassList.get(position).getId());
//
//        // Delete locally (TODO: needed?)
//        this.mClassList.remove(position);
//    }

    /**
     * Given a DataSnapshot of the quarter, returns a representation of that quarter's schedule.
     */
    static Quarter valueOf(String id, DataSnapshot snapshot) {
        List<SingleClass> classes = new ArrayList<>();
        for (DataSnapshot singleClass : snapshot.getChildren()) {
            classes.add(SingleClass.valueOf(singleClass));
        }
        return new Quarter(id, snapshot.getKey(), classes);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        ArrayList<String> classStrings = new ArrayList<>();
        for (SingleClass c : mClassList) {
            classStrings.add(c.serialize());
        }
        bundle.putString(USER_ID_KEY, mUserId);
        bundle.putString(QUARTER_ID_KEY, mId);
        bundle.putStringArrayList(CLASSES_KEY, classStrings);
        return bundle;
    }

    public static Quarter valueOf(Bundle bundle) throws RuntimeException {
        List<String> classStrings = bundle.getStringArrayList(CLASSES_KEY);
        if (classStrings == null) throw new RuntimeException("Malformed bundle for quarter.");
        List<SingleClass> classes = new ArrayList<>();
        for (int i = 0; i < classStrings.size(); i++) {
            classes.add(SingleClass.deserialize(classStrings.get(i)));
        }
        return new Quarter(bundle.getString(USER_ID_KEY), bundle.getString(QUARTER_ID_KEY), classes);
    }

    public String getClassId(int position) {
        return this.mClassList.get(position).getId();
    }
}
