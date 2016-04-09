package com.sarangjoshi.uwcalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        implements FirebaseData.UsersDownloadedListener, RequestScheduleFragment.NameSelectedListener, ChangePasswordFragment.ChangePasswordListener {
    // TODO: add persistent overall user listener, move changing aspects to separate objects in database

    FirebaseData mFirebaseData;
    GoogleAuthData mGoogleAuthData;

    ListView mRequestsList;
    TextView mIsConnected;
    Button mGoogleAuthBtn;

    ProgressDialog mDialog;

    private List<String> mRequests;

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

        mIsConnected = (TextView) findViewById(R.id.is_connected_to_google_view);

        // TODO: extract this into a one-time listener
        mFirebaseData.getUserRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child(FirebaseData.GOOGLEAUTH_KEY).exists()) {
                    // TODO: finish
                    connectedToGoogle(snapshot.child(FirebaseData.GOOGLEAUTH_KEY).getValue().toString());
                } else {
                    // NOT CONNECTED
                    mIsConnected.setText(getResources().getString(R.string.not_connected_to_google));
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
        mRequests = new ArrayList<>();

        mDialog = new ProgressDialog(this);
    }

    public void logoutClicked(View view) {
        mFirebaseData.getRef().unauth();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void requestScheduleClicked(View view) {
        // First need to download the data, then call the corresponding fragment
        if (mFirebaseData.getAllUsers().isEmpty())
            mFirebaseData.downloadAllUsers(HomeActivity.this);
        else
            onUsersDownloaded();
    }

    public void refreshUsersClicked(View view) {
        mDialog.setMessage("Refreshing users...");
        mDialog.show();
        mFirebaseData.downloadAllUsers(new FirebaseData.UsersDownloadedListener() {
            @Override
            public void onUsersDownloaded() {
                Toast.makeText(HomeActivity.this, "Users refreshed.", Toast.LENGTH_LONG).show();
                mDialog.hide();
            }
        });
    }

    public void viewClassesClicked(View view) {
        startActivity(new Intent(this, ScheduleActivity.class));
    }

    public void changePasswordClicked(View view) {
        DialogFragment changePassword = new ChangePasswordFragment();
        changePassword.show(getSupportFragmentManager(), "changePassword");
    }

    private void setRequestsData(DataSnapshot requests) {
        mRequests.clear();
        for (DataSnapshot request : requests.getChildren()) {
            mRequests.add(request.getValue().toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mRequests);
        mRequestsList.setAdapter(adapter);
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

    /**
     * To be called after connected to Google account.
     *
     * @param accountName the linked account name
     */
    private void connectedToGoogle(String accountName) {
        mIsConnected.setText(getResources().getString(R.string.connected_to_google));
        mGoogleAuthBtn.setVisibility(View.GONE);

        // Sets account name in the data object
        // TODO:
        mGoogleAuthData.setAccountName(accountName);
    }

    @Override
    public void onUsersDownloaded() {
        DialogFragment fragment = new RequestScheduleFragment();
        fragment.show(getSupportFragmentManager(), "requestSchedule");
    }

    @Override
    public void nameSelected(final RequestScheduleFragment.NameToId selected) {
        mDialog.setMessage("Requesting schedule...");
        // First check if a request has already been issued
        Firebase reqRef = mFirebaseData.getUsersRef().child(selected.id).child(FirebaseData.REQUESTS_KEY);
        reqRef.orderByValue().equalTo(mFirebaseData.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iter = dataSnapshot.getChildren().iterator();
                if (iter.hasNext()) {
                    // There is already a request
                    DataSnapshot child = iter.next();
                    Toast.makeText(HomeActivity.this, "Request has already been made.", Toast.LENGTH_LONG).show();
                } else {
                    // No request has been made so far.
                    // TODO: Check if a connection exists

                    // Add a request to the object
                    mFirebaseData.getUsersRef().child(selected.id).child(FirebaseData.REQUESTS_KEY).push().setValue(mFirebaseData.getUid());
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
}
