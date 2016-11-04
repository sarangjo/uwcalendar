package com.sarangjoshi.uwcalendar;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ExpandableListView;

import com.sarangjoshi.uwcalendar.adapters.DayListAdapter;
import com.sarangjoshi.uwcalendar.content.Day;
import com.sarangjoshi.uwcalendar.data.FirebaseData;
import com.sarangjoshi.uwcalendar.network.NetworkOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnectionActivity extends AppCompatActivity implements NetworkOps.ConnectionLoadedListener {
    private ExpandableListView mConnectionView;
    private ProgressDialog mDialog;

    private String mQuarter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mQuarter = getIntent().getStringExtra(AddClassActivity.QUARTER_KEY);

        // TODO; choose quarter via spinner

        // Views
        mConnectionView = (ExpandableListView) findViewById(R.id.connection_view);
        mDialog = new ProgressDialog(this);

        // Retrieve connection data
        mDialog.setMessage("Loading connection...");
        mDialog.show();

        NetworkOps ops = NetworkOps.getInstance();
        String id = getIntent().getStringExtra(FirebaseData.CONNECTION_ID_KEY);
        ops.retrieveConnection(id, this);
    }

    @Override
    public void connectionLoaded(Map<String, List<Day>> connection) {
        List<Day> week = connection.get(mQuarter);
        week = (week == null) ? new ArrayList<Day>() : week;
        DayListAdapter adapter = new DayListAdapter(this, week);
        mConnectionView.setAdapter(adapter);

        mDialog.hide();
    }
}
