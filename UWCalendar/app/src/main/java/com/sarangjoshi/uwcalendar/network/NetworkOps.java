package com.sarangjoshi.uwcalendar.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.ConnectionActivity;
import com.sarangjoshi.uwcalendar.content.Day;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Download a new Connection.
     */
    public void requestConnection(String id, final ConnectionLoadedListener listener) {
        FirebaseData.getInstance().getConnectionsRef().child(id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot connection) {
                ScheduleData sched = ScheduleData.getInstance();
                DataSnapshot data = connection.child(FirebaseData.DATA_KEY).child(sched.getCurrentQuarter());
                List<Day> week = new ArrayList<Day>();
                for (int i = 0; i < ScheduleData.DAYS_MAP.length; ++i) {
                    week.add(Day.valueOf(data.child(i + "")));
                }

                // Notify listener
                listener.connectionLoaded(week);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO lmao
            }
        });
    }

    public interface ConnectionLoadedListener {
        void connectionLoaded(List<Day> connection);
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
