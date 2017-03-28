package com.sarangjoshi.uwcalendar.content;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;
import com.sarangjoshi.uwcalendar.singletons.NetworkOps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 */
public class User {
    private static final String TAG = "User";
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

            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://uwcalendar.herokuapp.com/api/connect");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                Map<String, String> postParams = new HashMap<>();
                postParams.put("userA", fb.getUid());
                postParams.put("userB", requestToAccept.usernameAndId.id);
                postParams.put("request", requestToAccept.key);
                postParams.put("quarter", "sp16");

                OutputStream outputPost = new BufferedOutputStream(conn.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputPost, "UTF-8"));
                writer.write(getPostDataString(postParams));
                writer.flush();
                writer.close();
                outputPost.close();

                // Listen for response
                InputStream inputPost = new BufferedInputStream(conn.getInputStream());

                byte[] postResponse = new byte[512];
                int bytesRead = inputPost.read(postResponse);
                System.out.println(new String(Arrays.copyOfRange(postResponse, 0, bytesRead)));

                return true;
            } catch (IOException e) {
                String errorMessage = "";
                if (conn != null) {
                    try {
                        // Check if error code is bad request, then we get the error
                        if (conn.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                            InputStream errorPost = new BufferedInputStream(conn.getErrorStream());

                            byte[] postResponse = new byte[512];
                            int bytesRead = errorPost.read(postResponse);
                            errorMessage = new String(Arrays.copyOfRange(postResponse, 0, bytesRead));
                        }
                    } catch (IOException e2) {
                        e2.printStackTrace();
                        errorMessage = e2.getMessage();
                    }
                }
                if (errorMessage.isEmpty()) {
                    e.printStackTrace();
                    errorMessage = e.getMessage();
                }

                Log.d(TAG, "Error occurred: " + errorMessage);
                return false;
            }
        }

        /**
         * Converts from a map to a URL encoded String.
         */
        private String getPostDataString(Map<String, String> params) throws UnsupportedEncodingException {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (String key : params.keySet()) {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(params.get(key), "UTF-8"));
            }

            return result.toString();
        }
    }

    public void declineRequest(Context context, int position) {
        new DeclineRequestTask(context).execute(position);
    }

    /**
     * Decline a connection request. Parameters: position
     */
    private class DeclineRequestTask extends NetworkOps.OperationTask<Integer> {
        DeclineRequestTask(Context context) {
            super(context, "Declining request...");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            Request request = mRequests.get(params[0]);
            fb.getRequestsRef().child(fb.getUid()).child(request.key).removeValue();
            return true;
        }
    }

    public boolean deleteConnection(Context context, int position) {
        new DeleteConnectionTask(context).execute(position);
        return true;
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
            // TODO: 10/23/2016 actually delete the connection
            Toast.makeText(mContext, "This will be implemented soon!", Toast.LENGTH_LONG).show();
            return true;
        }
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

    public Connection getConnection(int position) {
        return mConnections.get(position);
    }
}
