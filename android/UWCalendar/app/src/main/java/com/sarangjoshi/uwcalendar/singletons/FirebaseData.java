package com.sarangjoshi.uwcalendar.singletons;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.content.SingleClass;

import java.util.ArrayList;
import java.util.List;

import static com.sarangjoshi.uwcalendar.activities.SaveToGoogleActivity.GOOGLE_EVENT_ID_KEY;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class FirebaseData {
    // User keys
    public static final String USERNAME_KEY = "username";
    public static final String GOOGLEAUTH_KEY = "googleauth";

    // Level 1 collections
    public static final String USERS_KEY = "users";
    public static final String SCHEDULES_KEY = "schedules";
    public static final String REQUESTS_KEY = "requests";
    public static final String CONNECTIONS_KEY = "connections";

    public static final String CONNECTION_ID_KEY = "connectionId";
    public static final String CONNECTION_WITH_KEY = "with";
    public static final String DATA_KEY = "data";
    public static final String START_KEY = "start";
    public static final String END_KEY = "end";
    public static final String CLASSES_KEY = "classes";
    public static final String TIME_KEY = "time";
    public static final String QUARTER_ID_KEY = "quarterId";

    private static FirebaseData ourInstance = new FirebaseData();

    public static FirebaseData getInstance() {
        return ourInstance;
    }

    private FirebaseData() {
        DatabaseReference mRef = FirebaseDatabase.getInstance().getReference();

        mAuth = FirebaseAuth.getInstance();

        mUsersRef = mRef.child(USERS_KEY);
        mSchedulesRef = mRef.child(SCHEDULES_KEY);
        mRequestsRef = mRef.child(REQUESTS_KEY);
        mConnectionsRef = mRef.child(CONNECTIONS_KEY);

        mUsersList = new ArrayList<>();
    }

    //// GLOBAL DATA
    private DatabaseReference mUsersRef;
    private DatabaseReference mSchedulesRef;
    private DatabaseReference mRequestsRef;
    private DatabaseReference mConnectionsRef;

    public DatabaseReference getUsersRef() {
        return mUsersRef;
    }

    public DatabaseReference getSchedulesRef() {
        return mSchedulesRef;
    }

    public DatabaseReference getConnectionsRef() {
        return mConnectionsRef;
    }

    public DatabaseReference getRequestsRef() {
        return mRequestsRef;
    }

    private List<UsernameAndId> mUsersList;

    //// CURRENT USER

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mCurrentUserRef;
    private DatabaseReference mCurrentScheduleRef;

    private ValueEventListener mScheduleListener;
    private ValueEventListener mRequestsListener;
    private ValueEventListener mConnectionsListener;

    /**
     * Gets the currently signed in user.
     *
     * @return null if no user is signed in
     */
    public FirebaseUser getUser() {
        return mCurrentUser;
    }

    /**
     * Gets a database reference to the currently signed user.
     */
    public DatabaseReference getCurrentUserRef() {
        return mCurrentUserRef;
    }

    public FirebaseAuth getFirebaseAuth() {
        return this.mAuth;
    }

    /**
     * Gets the currently signed-in user's universal Firebase ID.
     *
     * @return null if no user is currently logged in
     */
    public String getUid() {
        if (mCurrentUser != null) return mCurrentUser.getUid();
        return null;
    }

    /**
     * Adds the given class to the schedule.
     *
     * @param quarter     the quarter this class is in
     * @param singleClass the class to add
     */
    public void addClass(String quarter, SingleClass singleClass) {
        DatabaseReference s = mCurrentScheduleRef.child(quarter).push();
        s.setValue(singleClass);
        singleClass.setId(s.getKey());
    }

    public void saveGoogleEventId(String quarterId, SingleClass c) {
        mCurrentScheduleRef.child(quarterId).child(c.getId()).child(GOOGLE_EVENT_ID_KEY).setValue(c.getGoogleEventId());
    }

    /**
     * Remove a class from the Firebase.
     */
    public void removeClass(String quarterId, String classId) {
        getSchedulesRef().child(quarterId).child(classId).removeValue();
    }

    /**
     * Updates the current user. To be called on login.
     */
    public void updateCurrentUser() {
        this.mCurrentUser = mAuth.getCurrentUser();
        this.mCurrentUserRef = getUsersRef().child(getUid());
        this.mCurrentScheduleRef = getSchedulesRef().child(getUid());
    }

    //// FIREBASE LISTENERS

    /**
     * Sets a value listener for a Database reference.<br/>
     * Format: <code></code>oldVel = set(ref, oldVel, newVel);</code>
     */
    private ValueEventListener setValueListener(DatabaseReference ref, ValueEventListener oldVel, ValueEventListener newVel) {
        if (oldVel != null) {
            ref.removeEventListener(oldVel);
        }
        ref.addValueEventListener(newVel);
        return newVel;
    }

    /**
     * Sets a schedule listener for the current logged-in user.
     */
    public void setScheduleValueListener(ValueEventListener vel) {
        mScheduleListener = setValueListener(getSchedulesRef().child(getUid()), mScheduleListener, vel);
    }

    public void setRequestsValueListener(ValueEventListener vel) {
        mRequestsListener = setValueListener(getRequestsRef().child(getUid()), mRequestsListener, vel);
    }

    public void setConnectionsValueListener(ValueEventListener vel) {
        mConnectionsListener = setValueListener(getCurrentUserRef().child(CONNECTIONS_KEY), mConnectionsListener, vel);
    }

    //// OTHER MISC. USER DATA

    public void setUsers(List<UsernameAndId> users) {
        this.mUsersList = users;
    }

    /**
     * Assuming that the users have already been downloaded.
     */
    public List<UsernameAndId> getAllUsers() {
        return mUsersList;
    }

    /**
     * Finds the name given an id.
     * TODO make faster?
     */
    public UsernameAndId getUsernameAndIdFromId(String id) {
        for (UsernameAndId uandi : getAllUsers()) {
            if (uandi.id.equals(id)) {
                return uandi;
            }
        }
        return null;
    }

    public String getUsernameFromId(String id) {
        try {
            return getUsernameAndIdFromId(id).username;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Represents a 1-to-1 mapping between usernames and id's.
     */
    public static class UsernameAndId {
        public String username;
        public String id;

        public UsernameAndId(String username, String id) {
            this.username = username;
            this.id = id;
        }

        public String toString() {
            return "Username " + username + ", id " + id;
        }
    }
}
