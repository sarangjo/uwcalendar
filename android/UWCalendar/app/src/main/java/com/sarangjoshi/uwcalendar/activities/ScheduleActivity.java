package com.sarangjoshi.uwcalendar.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.sarangjoshi.uwcalendar.R;
import com.sarangjoshi.uwcalendar.models.Schedule;
import com.sarangjoshi.uwcalendar.models.SingleClass;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;
import com.sarangjoshi.uwcalendar.singletons.ScheduleData;
import com.sarangjoshi.uwcalendar.singletons.NetworkOps;

public class ScheduleActivity extends AppCompatActivity implements NetworkOps.ScheduleLoadedListener {
    public static final int ADD_CLASS_REQUEST = 2001;

    private ListView mClassesList;

    private Schedule mSchedule;
    private String mQuarter;

    private FirebaseData fb;
    // TODO: progress dialog until schedule loads

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Handle adding class intent
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivityForResult(new Intent(ScheduleActivity.this, AddClassActivity.class), ADD_CLASS_REQUEST);
                }
            });
        }

        // Singletons
        ScheduleData sched = ScheduleData.getInstance();

        mQuarter = sched.getCurrentQuarter();
        final String[] quarterCodes = sched.getQuarters();

        // Quarters
        Spinner spinner = (Spinner) findViewById(R.id.quarter_spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item, quarterCodes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(quarterCodes.length - 1);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mQuarter = quarterCodes[i];
                if (mSchedule != null)
                    onScheduleLoaded(mSchedule);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Listen to schedule changes
        NetworkOps.getInstance().retrieveSchedule(this);

        mClassesList = (ListView) findViewById(R.id.classes_list);
        mClassesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new NetworkOps.OperationTask<Integer>(ScheduleActivity.this, "Deleting...") {
                    @Override
                    protected Boolean doInBackground(Integer... params) {
                        // Get class ID from schedule
                        String id = mSchedule.getClassId(mQuarter, position);
                        fb.removeClass(mQuarter, id);

                        return true;
                    }
                }.execute(position);
                return true;
            }
        });

        fb = FirebaseData.getInstance();
    }

    public void onSaveToGoogleClicked(View view) {
        if (mSchedule != null) {
            Intent i = new Intent(this, SyncWithGoogleActivity.class);
            i.putExtras(mSchedule.getQuarter(mQuarter).toBundle());
            startActivity(i);
        } else {
            Toast.makeText(this, "Schedule not loaded", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_CLASS_REQUEST:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // Save class to database
                    SingleClass singleClass = SingleClass.valueOf(data);
                    new NetworkOps.OperationTask<SingleClass>(this, "Saving...") {
                        protected Boolean doInBackground(SingleClass... params) {
                            SingleClass singleClass = params[0];
                            fb.addClass(mQuarter, singleClass);
                            return true;
                        }
                    }.execute(singleClass);
                }
                break;
        }
    }

    @Override
    public void onScheduleLoaded(Schedule s) {
        mSchedule = s;
        ArrayAdapter<SingleClass> adapter = new ArrayAdapter<SingleClass>(ScheduleActivity.this,
                android.R.layout.simple_list_item_1, mSchedule.getClasses(mQuarter));
        mClassesList.setAdapter(adapter);
    }
}
