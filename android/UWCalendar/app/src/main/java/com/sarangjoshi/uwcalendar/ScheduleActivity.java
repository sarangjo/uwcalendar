package com.sarangjoshi.uwcalendar;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.sarangjoshi.uwcalendar.content.Schedule;
import com.sarangjoshi.uwcalendar.content.SingleClass;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;
import com.sarangjoshi.uwcalendar.network.NetworkOps;

import java.io.IOException;
import java.util.Set;

public class ScheduleActivity extends AppCompatActivity implements NetworkOps.ScheduleLoadedListener {
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

        // Handle adding class intent
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ScheduleActivity.this, AddClassActivity.class);
                    intent.putExtra(AddClassActivity.QUARTER_KEY, mQuarter);
                    startActivityForResult(intent, ADD_CLASS_REQUEST);
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
                    scheduleLoaded(mSchedule);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Listen to schedule changes
        NetworkOps.getInstance().requestSchedule(this);

        mClassesList = (ListView) findViewById(R.id.classes_list);
        mClassesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                new NetworkOps.OperationTask<Integer>(ScheduleActivity.this, "Deleting...") {
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
                    new NetworkOps.OperationTask<Object>(this, "Saving...") {
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

    @Override
    public void scheduleLoaded(Schedule s) {
        mSchedule = s;
        ArrayAdapter<SingleClass> adapter = new ArrayAdapter<SingleClass>(ScheduleActivity.this,
                android.R.layout.simple_list_item_1, mSchedule.getClasses(mQuarter));
        mClassesList.setAdapter(adapter);
    }
}
