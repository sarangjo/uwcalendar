package com.sarangjoshi.uwcalendar.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.ConnectionActivity;
import com.sarangjoshi.uwcalendar.HomeActivity;
import com.sarangjoshi.uwcalendar.content.Day;
import com.sarangjoshi.uwcalendar.content.Request;
import com.sarangjoshi.uwcalendar.content.Schedule;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sarangjoshi.uwcalendar.data.FirebaseData.USERNAME_KEY;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class NetworkOps {
    private static NetworkOps ourInstance = new NetworkOps();

    public static NetworkOps getInstance() {
        return ourInstance;
    }

    private NetworkOps() {
    }

    private FirebaseData fb = FirebaseData.getInstance();

    /**
     * Download a new Connection.
     *
     * @param id connection id
     */
    public void requestConnection(String id, final ConnectionLoadedListener listener) {
        FirebaseData.getInstance().getConnectionsRef().child(id).child(FirebaseData.DATA_KEY).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot connection) {
                Map<String, List<Day>> connectionByQuarters = new HashMap<>();

                for (DataSnapshot child : connection.getChildren()) {
                    List<Day> week = new ArrayList<>();
                    for (int i = 0; i < ScheduleData.DAYS_MAP.length; ++i) {
                        week.add(Day.valueOf(child.child(i + "")));
                    }
                    connectionByQuarters.put(child.getKey(), week);
                }

                // Notify listener
                listener.connectionLoaded(connectionByQuarters);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO lmao
            }
        });
    }

    public interface ConnectionLoadedListener {
        void connectionLoaded(Map<String, List<Day>> connection);
    }

    /**
     * Download a new Schedule.
     */
    public void requestSchedule(final ScheduleLoadedListener listener) {
        final FirebaseData fb = FirebaseData.getInstance();
        fb.setScheduleValueListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listener.scheduleLoaded(Schedule.valueOf(fb.getUid(), snapshot));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Download error", databaseError.getMessage());
            }
        });
    }

    public interface ScheduleLoadedListener {
        void scheduleLoaded(Schedule s);
    }

    public void requestUsers(final UsersLoadedListener listener) {
        // Global name<-->id one to one mapping
        fb.getUsersRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot users) {
                List<FirebaseData.UsernameAndId> usersList = new ArrayList<>();
                for (DataSnapshot user : users.getChildren()) {
                    usersList.add(new FirebaseData.UsernameAndId(user.child(USERNAME_KEY).getValue().toString(),
                            user.getKey()));
                }
                listener.usersLoaded(usersList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: lmao
            }
        });
    }

    public interface UsersLoadedListener {
        void usersLoaded(List<FirebaseData.UsernameAndId> usersList);
    }

    /**
     * Represents an asynchronous operation.
     *
     * @param <T> the input type
     */
    public static abstract class OperationTask<T> extends AsyncTask<T, Void, Boolean> {
        private ProgressDialog mDialog;
        private String mMessage;

        public OperationTask(Context context, String message) {
            this.mDialog = new ProgressDialog(context);
            this.mMessage = message;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.mDialog.setMessage(mMessage);
            this.mDialog.show();
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mDialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            mDialog.hide();
        }
    }
}
