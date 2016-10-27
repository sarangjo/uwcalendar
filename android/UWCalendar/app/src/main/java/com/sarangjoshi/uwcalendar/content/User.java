package com.sarangjoshi.uwcalendar.content;

import android.content.Context;
import android.os.AsyncTask;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.HomeActivity;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.network.NetworkOps;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 10/26/2016.
 */
public class User implements Schedule.RetrieveSchedulesListener {
    private static User ourInstance = new User();

    public static User getInstance() {
        return ourInstance;
    }

    // Model
    private List<Request> mRequests;
    private List<Connection> mConnections;
    private FirebaseData fb = FirebaseData.getInstance();

    private User() {
        mRequests = new ArrayList<>();
        mConnections = new ArrayList<>();
    }

    /**
     * Accepts the request at the given position.
     */
    public void acceptRequest(Context context, int position) {
        new AcceptRequestTask(context).execute(position);
    }

    @Override
    public void schedulesRetrieved(Request r, Schedule schedule1, Schedule schedule2) {

    }

    public Connection getConnection(int position) {
        return mConnections.get(position);
    }

    public boolean deleteConnection(Context context, int position) {
        new DeleteConnectionTask(context).execute(position);
        return true;
    }

    public void setRequests(DataSnapshot snapshot) {
        mRequests.clear();
        for (DataSnapshot request : snapshot.getChildren()) {
            String id = request.getValue().toString();
            mRequests.add(new Request(request.getKey(), fb.getUsernameAndIdFromId(id)));
        }
    }

    public void setConnections(DataSnapshot snapshot) {
        mConnections.clear();
        for (DataSnapshot conn : snapshot.getChildren()) {
            try {
                String id = conn.child(FirebaseData.CONNECTION_ID_KEY).getValue().toString();
                FirebaseData.UsernameAndId with = fb.getUsernameAndIdFromId(conn.child(FirebaseData.CONNECTION_WITH_KEY).getValue().toString());
                if (with != null) {
                    // Only add the connection if the name has loaded
                    mConnections.add(new Connection(id, with));
                }
            } catch (NullPointerException ignored) {
                // TODO what is causing this?
            }
        }
    }

    public List<Request> getRequests() {
        return mRequests;
    }

    public List<Connection> getConnections() {
        return mConnections;
    }

    /**
     * Accept a connection request. Parameters: position
     */
    private class AcceptRequestTask extends NetworkOps.OperationTask<Integer> {
        AcceptRequestTask(Context context) {
            super(context, "Accepting request...");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            Request requestToAccept = mRequests.get(params[0]);

            // First, request schedules to combine
            Schedule.request(requestToAccept, User.this);

            return null;
        }
    }

    /**
     * Deletes a connection.
     */
    private class DeleteConnectionTask extends NetworkOps.OperationTask<Integer> {
        DeleteConnectionTask(Context context) {
            super(context, "Deleting connection...");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            // TODO: 10/23/2016 actually delete the task
            int position = params[0];
            return null;
        }
    }

}
