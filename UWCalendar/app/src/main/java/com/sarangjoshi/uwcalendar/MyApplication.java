package com.sarangjoshi.uwcalendar;

import android.app.Application;

import com.firebase.client.Firebase;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
