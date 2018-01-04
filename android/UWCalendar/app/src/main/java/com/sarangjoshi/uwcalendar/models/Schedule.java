package com.sarangjoshi.uwcalendar.models;

import com.google.firebase.database.DataSnapshot;

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
     * Converts a DataSnapshot into a Schedule object.
     */
    public static Schedule valueOf(String id, DataSnapshot snapshot) {
        Schedule s = new Schedule(id);
        for (DataSnapshot qtrSnapshot : snapshot.getChildren()) {
            s.mQuarters.put(qtrSnapshot.getKey(), Quarter.valueOf(id, qtrSnapshot));
        }
        return s;
    }

    public String getClassId(String qtrId, int position) {
        return getQuarter(qtrId).getClassId(position);
    }
}
