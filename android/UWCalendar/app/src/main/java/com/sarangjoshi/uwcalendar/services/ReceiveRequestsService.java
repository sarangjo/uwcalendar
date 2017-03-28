package com.sarangjoshi.uwcalendar.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.singletons.FirebaseData;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: class comment
 */
public class ReceiveRequestsService extends IntentService {
    public static final String KEYS = "keys";
    public static final String IDS = "ids";
    public static final String BROADCAST_ACTION =
            "com.sarangjoshi.uwcalendar.REQUESTS_BROADCAST";

    FirebaseData fb = FirebaseData.getInstance();

    public ReceiveRequestsService() {
        super("name");
    }

    public ReceiveRequestsService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Listen to user request changes
        fb.setRequestsValueListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // TODO: Update requests
                updateRequestsData(snapshot);
            }

            @Override
            public void onCancelled(DatabaseError DatabaseError) {
                Log.d("Download error", DatabaseError.getMessage());
            }
        });
    }

    private void updateRequestsData(DataSnapshot snapshot) {
        List<String> requestKeys = new ArrayList<>();
        List<String> requestIds = new ArrayList<>();
        for (DataSnapshot request : snapshot.getChildren()) {
            String id = request.getValue().toString();
            // Saves the key for deletion later
            requestKeys.add(request.getKey());
            requestIds.add(id);
        }
        Intent localIntent = new Intent(BROADCAST_ACTION)
                .putExtra(KEYS, (String[]) requestKeys.toArray())
                .putExtra(IDS, (String[]) requestIds.toArray());
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    // Broadcast receiver for receiving status updates from the IntentService
    private class ResponseReceiver extends BroadcastReceiver
    {
        // Prevents instantiation
        private ResponseReceiver() {
        }

        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {

        }
    }
}

/**
 * TODO remove, this is used when the eventual service sends the data through an intent
 */
//    private void updateRequests(Intent intent) {
//        mRequests.clear();
//
//        String[] keys = intent.getStringArrayExtra(ReceiveRequestsService.KEYS);
//        String[] ids = intent.getStringArrayExtra(ReceiveRequestsService.IDS);
//
//        for (int i = 0; i < keys.length; ++i) {
//            mRequests.add(new Request(keys[i], fb.getUsernameAndIdFromId(ids[i])));
//        }
//
//        ArrayAdapter<Request> adapter = new ArrayAdapter<Request>(this, android.R.layout.simple_list_item_1, mRequests) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                TextView v = (TextView) super.getView(position, convertView, parent);
//                v.setText(getItem(position).usernameAndId.username);
//                return v;
//            }
//        };
//
//        mRequestsList.setAdapter(adapter);
//    }
