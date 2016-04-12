package com.sarangjoshi.uwcalendar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.ui.FirebaseListAdapter;
import com.sarangjoshi.uwcalendar.data.FirebaseData;

public class ConnectionActivity extends AppCompatActivity {
    FirebaseData mFirebaseData;

    private String mId;
    private FirebaseListAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        mFirebaseData = FirebaseData.getInstance();

        mId = getIntent().getStringExtra(FirebaseData.CONNECTION_ID_KEY);

        Firebase participantsRef = mFirebaseData.getConnectionsRef().child(mId).child(FirebaseData.PARTICIPANTS_KEY);

        mAdapter = new FirebaseListAdapter<String>(this, String.class, android.R.layout.simple_list_item_1, participantsRef) {
            @Override
            protected void populateView(View view, String id, int i) {
                ((TextView) view).setText(mFirebaseData.getNameAndIdFromId(id).name);
            }
        };
        ((ListView)findViewById(R.id.participants_list)).setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.cleanup();
    }
}
