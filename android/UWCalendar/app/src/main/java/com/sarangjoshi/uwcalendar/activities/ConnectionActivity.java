package com.sarangjoshi.uwcalendar.activities;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;

import com.sarangjoshi.uwcalendar.R;
import com.sarangjoshi.uwcalendar.adapters.DayListAdapter;
import com.sarangjoshi.uwcalendar.content.Day;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.data.ScheduleData;
import com.sarangjoshi.uwcalendar.network.NetworkOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The activity where the connection is shown to the user.
 */
public class ConnectionActivity extends AppCompatActivity implements NetworkOps.ConnectionLoadedListener {
    private ExpandableListView mConnectionView;
    private ProgressDialog mDialog;

    private Map<String, List<Day>> mConnection;
    private String mQuarter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up action bar/menu for this activity
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);

        mQuarter = getIntent().getStringExtra(AddClassActivity.QUARTER_KEY);

        // TODO; choose quarter via spinner

        // Views
        mConnectionView = (ExpandableListView) findViewById(R.id.connection_view);
        mDialog = new ProgressDialog(this);

        // Retrieve connection data
        mDialog.setMessage("Loading connection...");
        mDialog.show();

        final String[] quarterCodes = ScheduleData.getInstance().getQuarters();

        // Quarters
        Spinner spinner = (Spinner) findViewById(R.id.quarter_spinner);
        assert spinner != null;

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_item, quarterCodes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(quarterCodes.length - 1);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mQuarter = quarterCodes[i];
                if (mConnection != null)
                    updateConnectionView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        NetworkOps ops = NetworkOps.getInstance();
        String id = getIntent().getStringExtra(FirebaseData.CONNECTION_ID_KEY);
        ops.retrieveConnection(id, this);
    }

    /**
     * Updates the connection view.
     */
    private void updateConnectionView() {
        List<Day> week = mConnection.get(mQuarter);
        week = (week == null) ? new ArrayList<Day>() : week;
        DayListAdapter adapter = new DayListAdapter(this, week);
        mConnectionView.setAdapter(adapter);
    }

    @Override
    public void onConnectionLoaded(Map<String, List<Day>> connection) {
        this.mConnection = connection;
        updateConnectionView();
        mDialog.hide();
    }
}
