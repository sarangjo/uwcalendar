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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.sarangjoshi.uwcalendar.content.SingleClass;

import java.util.List;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {
    private static final int ADD_CLASS_REQUEST = 1;

    FirebaseData mFirebaseData;

    ListView mClassesView;

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
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent intent = new Intent(HomeActivity.this, AddClassActivity.class);
                startActivityForResult(intent, ADD_CLASS_REQUEST);
            }
        });

        mFirebaseData = FirebaseData.getInstance();
        TextView emailTextView = (TextView) findViewById(R.id.emailTextView);
        emailTextView.setText(mFirebaseData.getEmail());

        mClassesView = (ListView) findViewById(R.id.classesList);

        downloadSchedule();
    }

    /**
     * Downloads the schedule and then loads it onto the view.
     */
    private void downloadSchedule() {
        mFirebaseData.getUserRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                DataSnapshot schedule = snapshot.child("schedule");
                setClassesData(schedule);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d("Download error", firebaseError.getMessage());
            }
        });
    }

    /**
     * Sets the classes view data.
     *
     * @param schedule
     */
    private void setClassesData(DataSnapshot schedule) {
        List<SingleClass> list = new ArrayList<>();
        for (DataSnapshot quarter : schedule.getChildren()) {
            for (DataSnapshot singleClass : quarter.getChildren()) {
                list.add(singleClass.getValue(SingleClass.class));
            }
        }
        // TODO actually set view data
        ArrayAdapter<SingleClass> adapter = new ArrayAdapter<SingleClass>(this, android.R.layout.simple_list_item_1, list);
        mClassesView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ADD_CLASS_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Save class to database
                SingleClass singleClass = new SingleClass(
                        data.getStringExtra("name"),
                        data.getStringExtra("location"),
                        data.getIntExtra("days", 0),
                        data.getStringExtra("start"),
                        data.getStringExtra("end")
                );
                new SaveClassTask().execute(data.getStringExtra("quarter"), singleClass);
            }
        }
    }

    /**
     * Saves a single class. Parameters: quarter, SingleClass
     */
    private class SaveClassTask extends AsyncTask<Object, Void, Boolean> {
        private ProgressDialog dialog;

        public SaveClassTask() {
            this.dialog = new ProgressDialog(HomeActivity.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            mFirebaseData.addClass(params[0].toString(), (SingleClass) params[1]);

            return true;
        }

        protected void onPostExecute(final Boolean success) {
            dialog.dismiss();
            downloadSchedule();
        }
    }
}
