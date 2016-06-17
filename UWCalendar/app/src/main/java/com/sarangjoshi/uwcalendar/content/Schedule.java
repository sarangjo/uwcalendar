package com.sarangjoshi.uwcalendar.content;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.HomeActivity;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.GoogleAuthData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

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
    // Singletons
    GoogleAuthData goog;
    FirebaseData fb;

    // Data
    Map<String, Quarter> mQuarters;

    public Schedule() {
        mQuarters = new HashMap<>();

        goog = GoogleAuthData.getInstance();
        fb = FirebaseData.getInstance();
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
        if (!mQuarters.containsKey(qtr))
            mQuarters.put(qtr, new Quarter(qtr));
        return mQuarters.get(qtr).getClasses();
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
     * Requests the two given id's from Firebase.
     */
    public static void request(final HomeActivity.Request r, final RetrieveSchedulesListener listener) {
        // Request
        final FirebaseData fb = FirebaseData.getInstance();
        fb.getSchedulesRef().child(fb.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final Schedule schedule1 = Schedule.valueOf(dataSnapshot);
                        fb.getSchedulesRef().child(r.usernameAndId.id)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Schedule schedule2 = Schedule.valueOf(dataSnapshot);
                                        listener.schedulesRetrieved(r, schedule1, schedule2);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError firebaseError) {
                                        // TODO: lmao as always
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(DatabaseError firebaseError) {
                        // TODO: lmao as always
                    }
                });
    }

    /**
     * Converts a DataSnapshot into a Schedule object.
     */
    public static Schedule valueOf(DataSnapshot snapshot) {
        Schedule s = new Schedule();
        for (DataSnapshot qtrSnapshot : snapshot.getChildren()) {
            s.mQuarters.put(qtrSnapshot.getKey(), Quarter.valueOf(qtrSnapshot));
        }
        return s;
    }

    /**
     * Connects two schedules.
     *
     * @return a map from quarters to connected schedule weeks (represented as Lists of Days
     */
    public static Map<String, List<Day>> connect(Schedule schedule1, Schedule schedule2) {
        Map<String, List<Day>> map = new HashMap<>();

        //for (String qtr : .getQuarters()) {
        String qtr = ScheduleData.getInstance().getCurrentQuarter();
        List<Day> combinedQtr = Quarter.connect(schedule1.mQuarters.get(qtr), schedule2.mQuarters.get(qtr));
        map.put(qtr, combinedQtr);
        //}

        return map;
    }

    /**
     * An interface that listens for schedule retrieving
     */
    public interface RetrieveSchedulesListener {
        void schedulesRetrieved(HomeActivity.Request r, Schedule schedule1, Schedule schedule2);
    }
}
