package com.sarangjoshi.uwcalendar.content;

/**
 * TODO: Class comment
 */

import com.sarangjoshi.uwcalendar.data.FirebaseData;

/**
 * <b>Request</b> represents a request made by one user to another.
 */
public class Request {
    public String key;
    /**
     * The recipient user for this request.
     */
    public FirebaseData.UsernameAndId usernameAndId;

    public Request(String key, FirebaseData.UsernameAndId usernameAndId) {
        this.key = key;
        this.usernameAndId = usernameAndId;
    }
}
