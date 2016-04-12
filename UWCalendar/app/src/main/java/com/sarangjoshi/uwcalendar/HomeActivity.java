package com.sarangjoshi.uwcalendar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.GoogleAuthData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity
        implements RequestScheduleFragment.NameSelectedListener, ChangePasswordFragment.ChangePasswordListener {
    private FirebaseData mFirebaseData;
    private GoogleAuthData mGoogleAuthData;

    private ListView mRequestsList;
    private ListView mConnectionsList;
    private TextView mIsGoogleConnected;
    private Button mGoogleAuthBtn;

    private ProgressDialog mDialog;

    private List<Request> mRequests;
    private List<ConnectionProperty> mConnections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFirebaseData = FirebaseData.getInstance();

        // Google auth
        mGoogleAuthBtn = (Button) findViewById(R.id.connect_to_google_btn);
        mGoogleAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GoogleAuthActivity.class);
                startActivityForResult(intent, GoogleAuthData.GOOGLE_AUTH_REQUEST);
            }
        });
        mGoogleAuthData = GoogleAuthData.getInstance();
        mGoogleAuthData.setupCredentials(getApplicationContext());

        mIsGoogleConnected = (TextView) findViewById(R.id.is_connected_to_google_view);

        // Get user-specific data
        mFirebaseData.getUserRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(FirebaseData.GOOGLEAUTH_KEY).exists()) {
                    // TODO: finish
                    connectedToGoogle(snapshot.child(FirebaseData.GOOGLEAUTH_KEY).getValue().toString());
                } else {
                    // NOT CONNECTED
                    mIsGoogleConnected.setText(getResources().getString(R.string.not_connected_to_google));
                }

                TextView nameTextView = (TextView) findViewById(R.id.name_text_view);
                try {
                    nameTextView.setText(snapshot.child(FirebaseData.NAME_KEY).getValue().toString());
                } catch (NullPointerException e) {
                    nameTextView.setText("Name error");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // TODO lel
            }
        });

        // Listen to user request changes
        ValueEventListener reqVEL = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // TODO: Update requests
                setRequestsData(snapshot);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d("Download error", firebaseError.getMessage());
            }
        };
        mFirebaseData.setRequestsValueListener(reqVEL);

        mRequestsList = (ListView) findViewById(R.id.requests_list);
        mRequestsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                DialogFragment dialog = new DialogFragment() {
                    @NonNull
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);

                        // Setup "accept decline" dialog
                        builder.setTitle("Accept request.").setMessage("Accept request?").setPositiveButton(" Accept", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Accept request
                                new AcceptRequestTask().execute(position);
                            }
                        }).setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                        return builder.create();
                    }
                };
                dialog.show(getSupportFragmentManager(), "acceptRequestFragment");
            }
        });
        mRequests = new ArrayList<>();

        // Listen to user connection changes
        ValueEventListener connVEL = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                setConnections(dataSnapshot);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };
        mFirebaseData.setConnectionsValueListener(connVEL);

        mConnectionsList = (ListView) findViewById(R.id.connections_list);
        mConnectionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: Open connection page
                Intent intent = new Intent(HomeActivity.this, ConnectionActivity.class);
                intent.putExtra(FirebaseData.CONNECTION_ID_KEY, mConnections.get(position).id);
                startActivity(intent);
            }
        });
        mConnections = new ArrayList<>();

        mDialog = new ProgressDialog(this);
    }

    private void setConnections(DataSnapshot connections) {
        mConnections.clear();
        for (DataSnapshot conn : connections.getChildren()) {
            // TODO What if names haven't loaded yet?
            String id = conn.child(FirebaseData.CONNECTION_ID_KEY).getValue().toString();
            FirebaseData.NameAndId with = mFirebaseData.getNameAndIdFromId(conn.child(FirebaseData.CONNECTION_WITH_KEY).getValue().toString());
            mConnections.add(new ConnectionProperty(id, with));
        }
        ArrayAdapter<ConnectionProperty> adapter = new ArrayAdapter<ConnectionProperty>(this, android.R.layout.simple_list_item_1, mConnections) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(getItem(position).with.name);
                return v;
            }
        };
        mConnectionsList.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case GoogleAuthData.GOOGLE_AUTH_REQUEST:
                if (resultCode == RESULT_OK) {
                    String accountName = data.getStringExtra(GoogleAuthData.ACCOUNT_NAME_KEY);

                    // Save to database
                    Map<String, Object> gAuth = new HashMap<>();
                    gAuth.put(FirebaseData.GOOGLEAUTH_KEY, accountName);
                    mFirebaseData.getUserRef().updateChildren(gAuth);

                    Toast.makeText(this, accountName, Toast.LENGTH_LONG).show();

                    connectedToGoogle(accountName);
                } else if (resultCode == RESULT_CANCELED) {
                    // TODO: do nothing?
                }
                break;
        }
    }

    //// BUTTONS CLICK RESPONSES ////

    public void logoutClicked(View view) {
        mFirebaseData.getRef().unauth();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void requestScheduleClicked(View view) {
        // First need to download the data, then call the corresponding fragment
        DialogFragment fragment = new RequestScheduleFragment();
        fragment.show(getSupportFragmentManager(), "requestSchedule");
    }

    public void viewClassesClicked(View view) {
        startActivity(new Intent(this, ScheduleActivity.class));
    }

    public void changePasswordClicked(View view) {
        DialogFragment changePassword = new ChangePasswordFragment();
        changePassword.show(getSupportFragmentManager(), "changePassword");
    }

    /**
     * Sets the requests data, to be called when a request is inbound.
     */
    private void setRequestsData(DataSnapshot requests) {
        mRequests.clear();
        for (DataSnapshot request : requests.getChildren()) {
            String id = request.getValue().toString();
            // Saves the key for deletion later
            mRequests.add(new Request(request.getKey(), mFirebaseData.getNameAndIdFromId(id)));
        }
        ArrayAdapter<Request> adapter = new ArrayAdapter<Request>(this, android.R.layout.simple_list_item_1, mRequests) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(getItem(position).nameAndId.name);
                return v;
            }
        };
        mRequestsList.setAdapter(adapter);
    }

    /**
     * To be called after connected to Google account.
     *
     * @param accountName the linked account name
     */
    private void connectedToGoogle(String accountName) {
        mIsGoogleConnected.setText(getResources().getString(R.string.connected_to_google));
        mGoogleAuthBtn.setVisibility(View.GONE);

        // Sets account name in the data object
        // TODO:
        mGoogleAuthData.setAccountName(accountName);
    }

    @Override
    public void nameSelected(final FirebaseData.NameAndId selected) {
        mDialog.setMessage("Requesting schedule...");
        // First check if a request has already been issued
        Firebase reqRef = mFirebaseData.getRequestsRef().child(selected.id);
        reqRef.orderByValue().equalTo(mFirebaseData.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iter = dataSnapshot.getChildren().iterator();
                if (iter.hasNext()) {
                    // There is already a request
                    //DataSnapshot child = iter.next();
                    Toast.makeText(HomeActivity.this, "Request to " + selected.name + " has already been made.", Toast.LENGTH_LONG).show();
                } else {
                    // No request has been made so far.
                    // TODO: Check if a connection exists

                    // Add a request to the object
                    mFirebaseData.getRequestsRef().child(selected.id).push().setValue(mFirebaseData.getUid());
                    Toast.makeText(HomeActivity.this, "Request made to " + selected.name, Toast.LENGTH_LONG).show();
                }
                mDialog.hide();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // TODO as always
            }
        });
    }

    @Override
    public void passwordChanged(String oldPass, String newPass) {
        mDialog.setMessage("Changing password...");
        mDialog.show();
        mFirebaseData.getRef().changePassword(mFirebaseData.getEmail(), oldPass, newPass, new Firebase.ResultHandler() {
            @Override
            public void onSuccess() {
                // Password changed! Hurrah.
                Toast.makeText(HomeActivity.this, "Password changed.", Toast.LENGTH_LONG).show();
                mDialog.hide();
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                Toast.makeText(HomeActivity.this, "Error: " + firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                mDialog.hide();
            }
        });
    }

    /**
     * Accept a connection request. Parameters: position
     */
    private class AcceptRequestTask extends AsyncTask<Integer, Void, Void> {
        @Override
        protected void onPreExecute() {
            mDialog.setMessage("Accepting request...");
            mDialog.show();
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int position = params[0];

            // Add to the connections collection
            Map<String, Map<String, String>> conn = new HashMap<>();

            Request r = mRequests.get(position);

            Firebase connRef = mFirebaseData.getConnectionsRef().push();
            Firebase participantsRef = connRef.child(FirebaseData.PARTICIPANTS_KEY);

            participantsRef.push().setValue(r.nameAndId.id);
            participantsRef.push().setValue(mFirebaseData.getUid());

            /* Add to both users' connections list: */
            // Self
            Firebase userConn = mFirebaseData.getUserRef().child(FirebaseData.CONNECTIONS_KEY).push();
            userConn.child(FirebaseData.CONNECTION_ID_KEY).setValue(connRef.getKey());
            userConn.child(FirebaseData.CONNECTION_WITH_KEY).setValue(r.nameAndId.id);

            // Other
            userConn = mFirebaseData.getUsersRef().child(r.nameAndId.id).child(FirebaseData.CONNECTIONS_KEY).push();
            userConn.child(FirebaseData.CONNECTION_ID_KEY).setValue(connRef.getKey());
            userConn.child(FirebaseData.CONNECTION_WITH_KEY).setValue(mFirebaseData.getUid());

            // Delete the request
            mFirebaseData.getRequestsRef().child(mFirebaseData.getUid()).child(r.key).removeValue();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mDialog.hide();
        }
    }

    public class Request {
        public String key;
        public FirebaseData.NameAndId nameAndId;

        public Request(String key, FirebaseData.NameAndId nameAndId) {
            this.key = key;
            this.nameAndId = nameAndId;
        }
    }

    public class ConnectionProperty {
        public String id;
        public FirebaseData.NameAndId with;

        public ConnectionProperty(String id, FirebaseData.NameAndId with) {
            this.id = id;
            this.with = with;
        }
    }
}
