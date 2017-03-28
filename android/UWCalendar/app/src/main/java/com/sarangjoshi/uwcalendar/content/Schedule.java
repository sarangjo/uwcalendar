package com.sarangjoshi.uwcalendar.content;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;
import com.sarangjoshi.uwcalendar.singletons.GoogleAuth;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Schedule represents a quarterly class schedule.
 *
 * @author Sarang Joshi
 */
public class Schedule {
    // Data
    private Map<String, Quarter> mQuarters;
    private String mId;

    public Schedule(String id) {
        mQuarters = new HashMap<>();
        mId = id;
    }

    /**
     * Saves a new class to this schedule.
     */
    public void saveClass(String qtr, SingleClass singleClass) throws IOException {
        mQuarters.get(qtr).saveClass(singleClass);
    }

    /**
     * Returns an unmodifiable list of this schedule's classes, if the given quarter has a defined schedule.
     */
    public List<SingleClass> getClasses(String qtr) {
        // TODO: 11/4/2016 ??
        if (!mQuarters.containsKey(qtr))
            mQuarters.put(qtr, new Quarter(mId, qtr));
        return mQuarters.get(qtr).getClasses();
    }

    public Quarter getQuarter(String qtr) {
        return mQuarters.get(qtr);
    }

    /**
     * Deletes a class from the schedule.
     *
     * @param position the position in the classes list to delete
     * @return true on success; false otherwise
     * @throws IOException if the Google event could not be deleted successfully
     */
    public void deleteClass(String qtr, int position) throws IOException {
        mQuarters.get(qtr).removeClass(position);
    }


    /**
     * Converts a DataSnapshot into a Schedule object.
     */
    public static Schedule valueOf(String id, DataSnapshot snapshot) {
        Schedule s = new Schedule(id);
        for (DataSnapshot qtrSnapshot : snapshot.getChildren()) {
            s.mQuarters.put(qtrSnapshot.getKey(), Quarter.valueOf(id, qtrSnapshot));
        }
        return s;
    }
}
