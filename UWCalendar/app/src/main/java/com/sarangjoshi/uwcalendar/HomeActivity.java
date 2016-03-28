package com.sarangjoshi.uwcalendar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
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
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.sarangjoshi.uwcalendar.content.SingleClass;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.GoogleAuthData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    public static final int ADD_CLASS_REQUEST = 2001;
    public static final int GOOGLE_AUTH_REQUEST = 2002;
    public static final String ACCOUNT_NAME = "accountName";

    FirebaseData mFirebaseData;

    GoogleAuthData mGoogleAuthData;
    GoogleAccountCredential mCredential;

    ScheduleData mScheduleData;

    List<String> mClassIds;
    List<String> mQuarters;
    List<SingleClass> mSingleClassList;
    ListView mClassesList;
    TextView mIsConnected;
    Button mGoogleAuthBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomeActivity.this, AddClassActivity.class);
                startActivityForResult(intent, ADD_CLASS_REQUEST);
            }
        });

        mFirebaseData = FirebaseData.getInstance();
        TextView emailTextView = (TextView) findViewById(R.id.email_text_view);
        try {
            emailTextView.setText(mFirebaseData.getEmail());
        } catch (NullPointerException e) {
            emailTextView.setText("Email error");
        }

        // Google auth
        mGoogleAuthBtn = (Button) findViewById(R.id.connect_to_google_btn);
        mGoogleAuthBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GoogleAuthActivity.class);
                startActivityForResult(intent, GOOGLE_AUTH_REQUEST);
            }
        });
        mGoogleAuthData = GoogleAuthData.getInstance();
        mGoogleAuthData.setupCredentials(getApplicationContext());
        mCredential = mGoogleAuthData.getCredentials();

        mIsConnected = (TextView) findViewById(R.id.is_connected_to_google_view);

        // Schedule data
        mScheduleData = ScheduleData.getInstance();

        mFirebaseData.getUserRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.child("googleauth").exists()) {
                    // TODO: finish
                    connectedToGoogle(snapshot.child("googleauth").getValue().toString());
                } else {
                    // NOT CONNECTED
                    mIsConnected.setText(getResources().getString(R.string.not_connected_to_google));
                }

                DataSnapshot schedule = snapshot.child("schedule");
                setClassesData(schedule);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d("Download error", firebaseError.getMessage());
            }
        });

        mClassesList = (ListView) findViewById(R.id.classes_list);
        mClassIds = new ArrayList<>();
        mSingleClassList = new ArrayList<>();
        mQuarters = new ArrayList<>();
        mClassesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                new DeleteClassTask(position).execute();
                return true;
            }
        });
    }

    /**
     * Sets the classes view data.
     *
     * @param schedule
     */
    private void setClassesData(DataSnapshot schedule) {
        mSingleClassList.clear();
        mClassIds.clear();
        for (DataSnapshot quarter : schedule.getChildren()) {
            for (DataSnapshot singleClass : quarter.getChildren()) {
                mSingleClassList.add(singleClass.getValue(SingleClass.class));
                mClassIds.add(singleClass.getKey());
                mQuarters.add(quarter.getKey());
            }
        }
        ArrayAdapter<SingleClass> adapter = new ArrayAdapter<SingleClass>(this, android.R.layout.simple_list_item_1, mSingleClassList);
        mClassesList.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        switch (requestCode) {
            case ADD_CLASS_REQUEST:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // Save class to database and Google
                    SingleClass singleClass = new SingleClass(
                            data.getStringExtra("name"),
                            data.getStringExtra("location"),
                            data.getIntExtra("days", 0),
                            data.getStringExtra("start"),
                            data.getStringExtra("end")
                    );
                    new SaveClassTask().execute(data.getStringExtra("quarter"), singleClass);
                }
                break;
            case GOOGLE_AUTH_REQUEST:
                if (resultCode == RESULT_OK) {
                    String accountName = data.getStringExtra(ACCOUNT_NAME);

                    // Save to database
                    Map<String, Object> gAuth = new HashMap<>();
                    gAuth.put("googleauth", accountName);
                    mFirebaseData.getUserRef().updateChildren(gAuth);

                    Toast.makeText(this, accountName, Toast.LENGTH_LONG).show();

                    connectedToGoogle(accountName);
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
        if (mCredential.getSelectedAccountName() == null)
            mCredential.setSelectedAccountName(accountName);
    }

    /**
     * Saves a single class. Parameters: quarter, SingleClass
     */
    private class SaveClassTask extends AsyncTask<Object, Void, Boolean> {
        private Calendar mCalendarService = null;

        private ProgressDialog mDialog;

        public SaveClassTask() {
            this.mDialog = new ProgressDialog(HomeActivity.this);
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            this.mCalendarService = new Calendar.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("UW Calendar")
                    .build();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.mDialog.setMessage("Saving...");
            this.mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            SingleClass singleClass = (SingleClass) params[1];
            String quarter = params[0].toString();

            // Google
            try {
                // List the next 10 events from the primary calendar.
                Event event = getGoogleEvent(quarter, singleClass);

                String calendarId = "primary";
                event = mCalendarService.events().insert(calendarId, event).execute();

                Log.d("Event created:", event.getId());
                singleClass.setGoogleEventId(event.getId());
            } catch (Exception e) {
                cancel(true);
                return false;
            }

            // Firebase
            mFirebaseData.addClass(quarter, singleClass);

            return true;
        }

        /**
         * Manufacture a Google event from the given class details.
         *
         * @param singleClass
         * @return
         */
        private Event getGoogleEvent(String quarter, SingleClass singleClass) {
            Event event = new Event().setSummary(singleClass.getName())
                    .setLocation(singleClass.getLocation());

            String[] qtrDetails = mScheduleData.getQuarterInfo(quarter);

            // Expand start/end time to include full date
            // TODO: timezone
            String startString = qtrDetails[0] + "T" + singleClass.getStart() + ":00-07:00";
            DateTime startDateTime = new DateTime(startString);
            EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("America/Los_Angeles");
            event.setStart(start);

            String endString = startString.substring(0, 11) + singleClass.getEnd() + startString.substring(16);
            DateTime endDateTime = new DateTime(endString);
            EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("America/Los_Angeles");
            event.setEnd(end);

            // RECURRENCE DETAILS
            String enddate = (qtrDetails[1]).replaceAll("-", "");
            String[] recurrenceDays = {"MO", "TU", "WE", "TH", "FR"};
            String days = "";
            for (int i = 0; i < recurrenceDays.length; i++) {
                if ((singleClass.getDays() & (1 << i)) != 0) {
                    days += (days.isEmpty() ? "" : ",") + recurrenceDays[i];
                }
            }
            String[] recurrence = new String[]{"RRULE:FREQ=WEEKLY;UNTIL=" + enddate + "T115959Z;WKST=SU;BYDAY=" + days};
            event.setRecurrence(Arrays.asList(recurrence));

            return event;
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

    private class DeleteClassTask extends AsyncTask<Void, Void, Boolean> {
        private int mPosition;
        private SingleClass mClass;

        private Calendar mCalendarService = null;

        private ProgressDialog mDialog;

        DeleteClassTask(int position) {
            this.mPosition = position;
            this.mClass = mSingleClassList.get(position);

            this.mDialog = new ProgressDialog(HomeActivity.this);
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            this.mCalendarService = new Calendar.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("UW Calendar")
                    .build();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            this.mDialog.setMessage("Deleting...");
            this.mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // First the Google event
            try {
                mCalendarService.events().delete("primary", mClass.getGoogleEventId()).execute();
            } catch (IOException e) {
                Log.d("Calendar delete error", e.getMessage());
                cancel(true);
                return false;
            }

            // Then the Database event
            mFirebaseData.getSchedule().child(mQuarters.get(mPosition) + "/" + mClassIds.get(mPosition))
                    .removeValue();
            return true;
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
