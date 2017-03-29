package com.sarangjoshi.uwcalendar.activities;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.model.Event;
import com.sarangjoshi.uwcalendar.R;
import com.sarangjoshi.uwcalendar.content.Quarter;
import com.sarangjoshi.uwcalendar.content.SingleClass;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;
import com.sarangjoshi.uwcalendar.singletons.GoogleAuth;
import com.sarangjoshi.uwcalendar.singletons.NetworkOps;

import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static com.sarangjoshi.uwcalendar.content.Quarter.QUARTER_ID_KEY;

public class SaveToGoogleActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks {
    public static final String GOOGLE_EVENT_ID_KEY = "googleEventId";
    private static final String TAG = "SaveToGoogleActivity";
    private NetworkOps net;
    private GoogleAuth goog;
    private FirebaseData fb;

    ProgressDialog mProgress;
    Quarter mQuarter;
    private String mQuarterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        setContentView(R.layout.activity_save_to_google);

        goog = GoogleAuth.getInstance();
        goog.initializeCredential(this);

        net = NetworkOps.getInstance();
        fb = FirebaseData.getInstance();

        // Retrieve selected quarter from the intent
        mQuarter = Quarter.valueOf(getIntent().getExtras());
        mQuarterId = getIntent().getStringExtra(QUARTER_ID_KEY);

        // Get going
        getResultsFromApi();
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!goog.isGooglePlayServicesAvailable(this)) {
            acquireGooglePlayServices();
        } else if (goog.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!net.isDeviceOnline(this)) {
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_LONG).show();
        } else {
            new MakeRequestTask().execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(GoogleAuth.REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
//            String accountName = getPreferences(Context.MODE_PRIVATE)
//                    .getString(GoogleAuth.ACCOUNT_NAME_KEY, null);
//            if (accountName != null) {
//                goog.setSelectedAccountName(accountName);
//                getResultsFromApi();
//            } else {
            // Start a dialog from which the user can choose an account
            startActivityForResult(goog.chooseAccount(), GoogleAuth.REQUEST_ACCOUNT_PICKER);
//            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(this,
                    "This app needs to access your Google account (via Contacts).",
                    GoogleAuth.REQUEST_PERMISSION_GET_ACCOUNTS, Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GoogleAuth.REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Google Play Services required.", Toast.LENGTH_LONG).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case GoogleAuth.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        // Save chosen account - TODO save to Firebase for this account?
//                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = settings.edit();
//                        editor.putString(GoogleAuth.ACCOUNT_NAME_KEY, accountName);
//                        editor.apply();

                        goog.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case GoogleAuth.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            goog.playServicesAvailabilityError(this, connectionStatusCode).show();
        }
    }

    /**
     * An asynchronous task that handles the Google Calendar API call to save the current quarter's schedule.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, Boolean> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        private MakeRequestTask() {
            // TODO move this to GoogleAuth?
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, goog.getCredential())
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                saveSchedule();
                return true;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return false;
            }
        }

        private void saveSchedule() {
            for (SingleClass c : mQuarter.getClasses()) {
                // First check to see if this event id already exists, in the case of double-saving
                String geId = c.getGoogleEventId();

                Event event = c.createGoogleEvent(mQuarterId);

                // TODO: add support for multiple calendar id's
                String calendarId = "primary";

                if (geId == null) {
                    try {
                        event = mService.events().insert(calendarId, event).execute();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to insert event.", e);
                        mLastError = e;
                        cancel(true);
                        return;
                    }

                    Log.d("Event created:", event.getId());
                    c.setGoogleEventId(event.getId());

                    // Save google event id on firebase
                    fb.saveGoogleEventId(mQuarterId, c);
                } else {
                    try {
                        mService.events().update(calendarId, geId, event);
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to update event.", e);
                        mLastError = e;
                        cancel(true);
                        return;
                    }

                    Log.d("Event updated:", event.getId());
                }

                // TODO: handle deleted events somehow
            }
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Boolean output) {
            mProgress.hide();
            Toast.makeText(SaveToGoogleActivity.this, "Saved classes to Google", Toast.LENGTH_LONG).show();

            finish();
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    goog.playServicesAvailabilityError(
                            SaveToGoogleActivity.this, ((GooglePlayServicesAvailabilityIOException) mLastError).getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            GoogleAuth.REQUEST_AUTHORIZATION);
                } else {
                    Log.e(TAG, "[MakeRequestTask] Error occurred: ", mLastError);
                }
            } else {
                Toast.makeText(SaveToGoogleActivity.this, "Request cancelled.", Toast.LENGTH_LONG).show();
            }
        }
    }
}