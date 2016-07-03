package com.sarangjoshi.uwcalendar.data;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sarangjoshi.uwcalendar.content.Segment;
import com.sarangjoshi.uwcalendar.content.SingleClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public static final String PARTICIPANTS_KEY = "participants";
    public static final String CONNECTION_ID_KEY = "connectionId";
    public static final String CONNECTION_WITH_KEY = "with";
    public static final String DATA_KEY = "data";
    public static final String START_KEY = "start";
    public static final String END_KEY = "end";
    public static final String CLASSES_KEY = "classes";

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

        // Global name<-->id one to one mapping
        getUsersRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot users) {
                mUsersList.clear();

                for (DataSnapshot user : users.getChildren()) {
                    mUsersList.add(new UsernameAndId(user.child(USERNAME_KEY).getValue().toString(),
                            user.getKey()));
                }

                if (mUsersLoadedListener != null) mUsersLoadedListener.usersLoaded();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // TODO: lmao
            }
        });
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

    private UsersLoadedListener mUsersLoadedListener;
    private List<UsernameAndId> mUsersList;

    ///// CURRENT USER

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mCurrentUserRef;

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
        getSchedulesRef().child(getUid()).child(quarter).push().setValue(singleClass);
    }

    /**
     * Remove a class from the Firebase.
     */
    public void removeClass(String quarterId, String classId) {
        getSchedulesRef().child(getUid() + "/" + quarterId + "/" + classId).removeValue();
    }

    /**
     * Updates the current user. To be called on login.
     */
    public void updateCurrentUser() {
        this.mCurrentUser = mAuth.getCurrentUser();
        this.mCurrentUserRef = getUsersRef().child(getUid());
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

    public void setUsersListener(UsersLoadedListener l) {
        this.mUsersLoadedListener = l;
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

    /**
     * Saves a combined segment in a connection, ONLY if the segment has at least
     * one class in it.
     */
    public void saveSegment(DatabaseReference segRef, Segment s) {
        segRef.child(START_KEY).setValue(String.format(Locale.US, "%02d:%02d", s.startHr, s.startMin));
        segRef.child(END_KEY).setValue(String.format(Locale.US, "%02d:%02d", s.endHr, s.endMin));
        DatabaseReference classesRef = segRef.child(CLASSES_KEY);

        for (String id : s.classesMap.keySet()) {
            classesRef.child(id).setValue(s.classesMap.get(id).getName());
        }
    }

    public String getUsernameFromId(String id) {
        try {
            return getUsernameAndIdFromId(id).username;
        } catch (Exception e) {
            return null;
        }
    }

    public interface UsersLoadedListener {
        void usersLoaded();
    }

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
