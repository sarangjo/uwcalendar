package com.sarangjoshi.uwcalendar.singletons;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.models.Day;
import com.sarangjoshi.uwcalendar.models.Schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sarangjoshi.uwcalendar.singletons.FirebaseData.USERNAME_KEY;

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
     * Retrieves a new Connection.
     *
     * @param id connection id
     */
    public void retrieveConnection(String id, final ConnectionLoadedListener listener) {
        FirebaseData.getInstance().getConnectionsRef().child(id).child(FirebaseData.DATA_KEY).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot connection) {
                // TODO: 11/4/2016 Update connection conversion
                Map<String, List<Day>> connectionByQuarters = new HashMap<>();

                for (DataSnapshot quarter : connection.getChildren()) {
                    List<Day> week = new ArrayList<>();
                    for (int i = 0; i < ScheduleData.DAYS_MAP.length; ++i) {
                        week.add(Day.valueOf(quarter.child(i + "")));
                    }
                    connectionByQuarters.put(quarter.getKey(), week);
                }

                // Notify listener
                listener.onConnectionLoaded(connectionByQuarters);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO lmao
            }
        });
    }

    public boolean isDeviceOnline(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public interface ConnectionLoadedListener {
        void onConnectionLoaded(Map<String, List<Day>> connection);
    }

    /**
     * Download a new Schedule.
     */
    public void retrieveSchedule(final ScheduleLoadedListener listener) {
        final FirebaseData fb = FirebaseData.getInstance();
        fb.setScheduleValueListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listener.onScheduleLoaded(Schedule.valueOf(fb.getUid(), snapshot));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Download error", databaseError.getMessage());
            }
        });
    }

    public interface ScheduleLoadedListener {
        void onScheduleLoaded(Schedule s);
    }

    /**
     * Download all users.
     */
    public void retrieveUsers(final UsersLoadedListener listener) {
        // Global name<-->id one to one mapping
        fb.getUsersRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot users) {
                List<FirebaseData.UsernameAndId> usersList = new ArrayList<>();
                for (DataSnapshot user : users.getChildren()) {
                    usersList.add(new FirebaseData.UsernameAndId(user.child(USERNAME_KEY).getValue().toString(),
                            user.getKey()));
                }
                listener.onUsersLoaded(usersList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: lmao
            }
        });
    }

    public interface UsersLoadedListener {
        void onUsersLoaded(List<FirebaseData.UsernameAndId> usersList);
    }

    /**
     * Represents an asynchronous operation.
     *
     * @param <T> the input type
     */
    public static abstract class OperationTask<T> extends AsyncTask<T, Void, Boolean> {
        private ProgressDialog mDialog;
        private String mMessage;
        protected Context mContext;

        public OperationTask(Context context, String message) {
            this.mDialog = new ProgressDialog(context);
            this.mContext = context;
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
            // TODO: 11/4/2016 Do something with the success
            mDialog.dismiss();
        }

        @Override
        protected void onCancelled() {
            mDialog.hide();
        }
    }
}
