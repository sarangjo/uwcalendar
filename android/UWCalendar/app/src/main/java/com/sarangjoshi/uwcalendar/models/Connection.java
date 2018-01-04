package com.sarangjoshi.uwcalendar.models;

import com.sarangjoshi.uwcalendar.singletons.FirebaseData;

/**
 * Representative of a connection between this user and another user.
 */
public class Connection {
    public String id;
    public FirebaseData.UsernameAndId with;

    public Connection(String id, FirebaseData.UsernameAndId with) {
        this.id = id;
        this.with = with;
    }
}