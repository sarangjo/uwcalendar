package com.sarangjoshi.uwcalendar;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.sarangjoshi.uwcalendar.content.SingleClass;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class FirebaseData {
    private static FirebaseData ourInstance = new FirebaseData();

    public static FirebaseData getInstance() {
        return ourInstance;
    }

    private FirebaseData() {
        mRef = new Firebase("https://uwcalendar.firebaseio.com");
        mUsersRef = mRef.child("users");
    }

    // Data
    private Firebase mRef;
    private Firebase mUsersRef;
    private Firebase mCurrentUserRef;
    private AuthData mAuthData;

    public Firebase getRef() {
        return mRef;
    }

    public Firebase getUsersRef() {
        return mUsersRef;
    }

    public Firebase getUserRef() {
        return mCurrentUserRef;
    }

    public String getEmail() {
        return mAuthData.getProviderData().get("email").toString();
    }

    /**
     * Adds the given class to the schedule.
     *
     * @param quarter
     * @param singleClass
     */
    public void addClass(String quarter, SingleClass singleClass) {
        this.mCurrentUserRef.child("schedule/" + quarter).push().setValue(singleClass);
    }

    public void saveAuthData(AuthData authData) {
        this.mAuthData = authData;
        this.mCurrentUserRef = getUsersRef().child(mAuthData.getUid());
    }

    public Firebase getSchedule() {
        return this.mCurrentUserRef.child("schedule");
    }
}
