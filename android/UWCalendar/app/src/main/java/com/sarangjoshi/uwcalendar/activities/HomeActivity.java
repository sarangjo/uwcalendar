package com.sarangjoshi.uwcalendar.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.R;
import com.sarangjoshi.uwcalendar.content.Connection;
import com.sarangjoshi.uwcalendar.content.Request;
import com.sarangjoshi.uwcalendar.content.User;
import com.sarangjoshi.uwcalendar.fragments.AcceptRequestDialogFragment;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;
import com.sarangjoshi.uwcalendar.fragments.ChangePasswordFragment;
import com.sarangjoshi.uwcalendar.fragments.RequestScheduleFragment;
import com.sarangjoshi.uwcalendar.singletons.NetworkOps;
import com.sarangjoshi.uwcalendar.singletons.ScheduleData;

import java.util.Iterator;
import java.util.List;

/**
 * The central activity for the application's workflow. Once a user is logged in, they reach the
 * home screen (here!) from where they can adjust their class schedules, issue connection requests
 * to other users, and view established connections with other users.
 */
public class HomeActivity extends AppCompatActivity
        implements RequestScheduleFragment.NameSelectedListener,
        ChangePasswordFragment.ChangePasswordListener,
        NetworkOps.UsersLoadedListener,
        AcceptRequestDialogFragment.AcceptRequestListener {
    private static final String TAG = "HomeActivity";

    // Singletons
    private FirebaseData fb;

    // View/Controller
    private ListView mRequestsList;
    private ListView mConnectionsList;

    private ProgressDialog mDialog;

    // Model
    private User mUser;

    //// ACTIVITY METHODS ////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fb = FirebaseData.getInstance();

        // TODO Trying out service
        /* startService(new Intent(this, ReceiveRequestsService.class)); */

        initializeViews();
        mUser = User.getInstance();

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.show();

        // The first thing to do is download user information
        NetworkOps.getInstance().retrieveUsers(this);
    }

    //// VIEW INITIALIZATION METHODS ////

    /**
     * Initializes the requests and connections list views.
     */
    private void initializeViews() {
        mRequestsList = (ListView) findViewById(R.id.requests_list);
        // When a request is clicked, ask the user whether they want to accept the request
        mRequestsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                DialogFragment dialog = new AcceptRequestDialogFragment();
                Bundle b = new Bundle();
                b.putInt(AcceptRequestDialogFragment.ACCEPT_REQUEST_POSITION, position);
                dialog.setArguments(b);
                dialog.show(getSupportFragmentManager(), "acceptRequestFragment");
            }
        });

        mConnectionsList = (ListView) findViewById(R.id.connections_list);
        // When a connection is clicked, open it
        mConnectionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Open connection page
                Intent intent = new Intent(HomeActivity.this, ConnectionActivity.class);
                intent.putExtra(FirebaseData.CONNECTION_ID_KEY, mUser.getConnection(position).id);
                intent.putExtra(FirebaseData.QUARTER_ID_KEY, ScheduleData.getInstance().getCurrentQuarter()); // TODO replace
                startActivity(intent);
            }
        });
        // TODO implement
        mConnectionsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mUser.deleteConnection(HomeActivity.this, position);
                return true;
            }
        });
    }

    @Override
    public void onUsersLoaded(List<FirebaseData.UsernameAndId> usersList) {
        fb.setUsers(usersList);
        mDialog.hide();

        // Get initial user-specific data
        fb.getCurrentUserRef().child(FirebaseData.USERNAME_KEY).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                TextView usernameTextView = (TextView) findViewById(R.id.username_text_view);
                if (usernameTextView != null)
                    try {
                        usernameTextView.setText(snapshot.getValue().toString());
                    } catch (NullPointerException e) {
                        usernameTextView.setText(getResources().getString(R.string.name_error));
                    }
            }

            @Override
            public void onCancelled(DatabaseError DatabaseError) {
                // TODO lel
                Log.d(TAG, "User download cancelled.");
            }
        });

        // Listen to user connection changes
        fb.setConnectionsValueListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                updateConnections(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError DatabaseError) {

            }
        });

        // Listen to user request changes
        fb.setRequestsValueListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                updateRequests(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError DatabaseError) {
                Log.d("Download error", DatabaseError.getMessage());
            }
        });
    }

    //// VIEW UPDATE METHODS ////

    /**
     * Updates the local requests list once they have been retrieved from the server.
     */
    private void updateRequests(DataSnapshot snapshot) {
        mUser.setRequests(snapshot);
        ArrayAdapter<Request> adapter = new ArrayAdapter<Request>(this, android.R.layout.simple_list_item_1, mUser.getRequests()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(getItem(position).usernameAndId.username);
                return v;
            }
        };
        mRequestsList.setAdapter(adapter);
    }

    /**
     * Sets the connections view from the DataSnapshot.
     */
    private void updateConnections(DataSnapshot connections) {
        mUser.setConnections(connections);
        ArrayAdapter<Connection> adapter = new ArrayAdapter<Connection>(this, android.R.layout.simple_list_item_1, mUser.getConnections()) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(getItem(position).with.username);
                return v;
            }
        };
        mConnectionsList.setAdapter(adapter);
    }

    //// BUTTONS CLICK RESPONSES ////

    public void logoutClicked(View view) {
        FirebaseAuth.getInstance().signOut();
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

    //// FRAGMENT RESPONSES ////

    @Override
    public void onScheduleRequested(final FirebaseData.UsernameAndId selected) {
        mDialog.setMessage("Requesting schedule...");
        mDialog.show();
        // First check if a request has already been issued
        DatabaseReference reqRef = fb.getRequestsRef().child(selected.id);
        reqRef.orderByValue().equalTo(fb.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iter = dataSnapshot.getChildren().iterator();
                if (iter.hasNext()) {
                    // There is already a request
                    Toast.makeText(HomeActivity.this, "Request to " + selected.username + " has already been made.", Toast.LENGTH_LONG).show();
                } else {
                    // No request has been made so far.
                    // TODO Now check if a connection exists
                    DatabaseReference connsRef = fb.getCurrentUserRef().child(FirebaseData.CONNECTIONS_KEY);
                    connsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                if (child.child(FirebaseData.CONNECTION_WITH_KEY).getValue().equals(selected.id)) {
                                    mDialog.hide();
                                    Toast.makeText(HomeActivity.this, "A connection with " + selected.username + " already exists.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                            // Add a request to the object
                            fb.getRequestsRef().child(selected.id).push().setValue(fb.getUid());
                            Toast.makeText(HomeActivity.this, "Request made to " + selected.username, Toast.LENGTH_LONG).show();
                            mDialog.hide();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // TODO: 11/5/2016 lelly
                            mDialog.hide();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError DatabaseError) {
                // TODO as always
                mDialog.hide();
            }
        });
    }

    @Override
    public void passwordChanged(String oldPass, String newPass) {
        // TODO: don't need the old password at all
        mDialog.setMessage("Changing password...");
        mDialog.show();
        fb.getUser().updatePassword(newPass)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Password changed! Hurrah.
                            Toast.makeText(HomeActivity.this, "Password changed.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(HomeActivity.this, "Error", Toast.LENGTH_LONG).show();
                        }
                        mDialog.hide();
                    }
                });
    }

    @Override
    public void acceptRequest(int position) {
        // Accept request
        mUser.acceptRequest(HomeActivity.this, position);
    }

    @Override
    public void declineRequest(int position) {
        // Decline request
        mUser.declineRequest(HomeActivity.this, position);
    }
}
