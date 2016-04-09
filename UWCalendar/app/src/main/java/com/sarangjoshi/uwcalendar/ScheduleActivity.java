package com.sarangjoshi.uwcalendar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.sarangjoshi.uwcalendar.content.SingleClass;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.GoogleAuthData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {
    public static final int ADD_CLASS_REQUEST = 2001;

    // Data singletons
    FirebaseData mFirebaseData;
    ScheduleData mScheduleData;
    GoogleAuthData mGoogleAuthData;

    List<String> mClassIds;
    List<String> mQuarters;
    List<SingleClass> mSingleClassList;
    ListView mClassesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ScheduleActivity.this, AddClassActivity.class);
                startActivityForResult(intent, ADD_CLASS_REQUEST);
            }
        });

        // Data objects
        mScheduleData = ScheduleData.getInstance();
        mFirebaseData = FirebaseData.getInstance();
        mGoogleAuthData = GoogleAuthData.getInstance();

        // Listen to schedule changes
        ValueEventListener scheduleVEL = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                setScheduleData(snapshot);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d("Download error", firebaseError.getMessage());
            }
        };
        mFirebaseData.setScheduleValueListener(scheduleVEL);

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
     * Once data has been retrieved from the database, we can load the relevant views.
     *
     * @param schedule the snapshot retrieved from the firebase
     */
    private void setScheduleData(DataSnapshot schedule) {
        // Class information
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
        }
    }

    /**
     * Saves a single class. Parameters: quarter, SingleClass
     */
    private class SaveClassTask extends AsyncTask<Object, Void, Boolean> {
        private com.google.api.services.calendar.Calendar mCalendarService = null;

        private ProgressDialog mDialog;

        public SaveClassTask() {
            this.mDialog = new ProgressDialog(ScheduleActivity.this);
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            this.mCalendarService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, mGoogleAuthData.getCredentials())
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

            // RECURRENCE DETAILS
            String[] recurrenceDays = {"MO", "TU", "WE", "TH", "FR"};
            String days = "";
            int offset = 0;
            for (int i = 0; i < recurrenceDays.length; i++) {
                if ((singleClass.getDays() & (1 << i)) != 0) {
                    if (days.isEmpty()) {
                        offset = i;
                    } else {
                        days += ",";
                    }
                    days += recurrenceDays[i];
                }
            }
            String endDate = (qtrDetails[1]).replaceAll("-", "");
            String[] recurrence = new String[]{"RRULE:FREQ=WEEKLY;UNTIL=" + endDate + "T115959Z;WKST=SU;BYDAY=" + days};
            event.setRecurrence(Arrays.asList(recurrence));

            // Expand start/end time to include full date
            // TODO: timezone
            String monday = qtrDetails[0];  // Start date
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(sdf.parse(monday));
            } catch (ParseException ignored) {
            }
            c.add(Calendar.DATE, offset);

            String startString = sdf.format(c.getTime()) + "T" + singleClass.getStart() + ":00-07:00";
            DateTime startDateTime = new DateTime(startString);
            EventDateTime start = new EventDateTime().setDateTime(startDateTime).setTimeZone("America/Los_Angeles");
            event.setStart(start);

            String endString = startString.substring(0, 11) + singleClass.getEnd() + startString.substring(16);
            DateTime endDateTime = new DateTime(endString);
            EventDateTime end = new EventDateTime().setDateTime(endDateTime).setTimeZone("America/Los_Angeles");
            event.setEnd(end);

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

        private com.google.api.services.calendar.Calendar mCalendarService = null;

        private ProgressDialog mDialog;

        DeleteClassTask(int position) {
            this.mPosition = position;
            this.mClass = mSingleClassList.get(position);

            this.mDialog = new ProgressDialog(ScheduleActivity.this);
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            this.mCalendarService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, mGoogleAuthData.getCredentials())
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
