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
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.content.Schedule;
import com.sarangjoshi.uwcalendar.content.SingleClass;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.io.IOException;

public class ScheduleActivity extends AppCompatActivity {
    public static final int ADD_CLASS_REQUEST = 2001;

    private ListView mClassesList;

    private Schedule mSchedule;
    private String mQuarter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ScheduleActivity.this, AddClassActivity.class);
                    startActivityForResult(intent, ADD_CLASS_REQUEST);
                }
            });
        }

        // Singletons
        ScheduleData sched = ScheduleData.getInstance();
        FirebaseData fb = FirebaseData.getInstance();

        mQuarter = sched.getCurrentQuarter();

        // Listen to schedule changes
        ValueEventListener scheduleVEL = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mSchedule = Schedule.valueOf(snapshot);
                ArrayAdapter<SingleClass> adapter = new ArrayAdapter<SingleClass>(ScheduleActivity.this,
                        android.R.layout.simple_list_item_1, mSchedule.getClasses(mQuarter));
                mClassesList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Download error", databaseError.getMessage());
            }
        };
        fb.setScheduleValueListener(scheduleVEL);

        mClassesList = (ListView) findViewById(R.id.classes_list);
        mSchedule = new Schedule();
        mClassesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                new OperationTask<Integer>("Deleting...") {
                    @Override
                    protected Boolean doInBackground(Integer... params) {
                        int position = params[0];
                        try {
                            mSchedule.deleteClass(mQuarter, position);
                        } catch (IOException e) {
                            Log.d("Calendar delete error", e.getMessage());
                            cancel(true);
                            return false;
                        }
                        return true;
                    }
                }.execute(position);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_CLASS_REQUEST:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // Save class to database and Google
                    SingleClass singleClass = SingleClass.valueOf(data);
                    new OperationTask<Object>("Saving...") {
                        protected Boolean doInBackground(Object... params) {
                            SingleClass singleClass = (SingleClass) params[1];
                            String quarter = params[0].toString();

                            try {
                                mSchedule.saveClass(quarter, singleClass);
                            } catch (IOException e) {
                                Log.d("Calendar save error", e.getMessage());
                                cancel(true);
                                return false;
                            }

                            return true;
                        }
                    }.execute(data.getStringExtra(AddClassActivity.QUARTER_KEY), singleClass);
                }
                break;
        }
    }

    /**
     * Represents an asynchronous operation.
     *
     * @param <T> the input type
     */
    private abstract class OperationTask<T> extends AsyncTask<T, Void, Boolean> {
        private ProgressDialog mDialog;
        private String mMessage;

        OperationTask(String message) {
            this.mDialog = new ProgressDialog(ScheduleActivity.this);
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
