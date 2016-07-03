package com.sarangjoshi.uwcalendar.data;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class GoogleAuthData {
    //public static final boolean GOOGLE_ENABLED = false;

    public static final int GOOGLE_AUTH_REQUEST = 2002;
    public static final String ACCOUNT_NAME_KEY = "accountName";

    private static GoogleAuthData ourInstance = new GoogleAuthData();

    public static GoogleAuthData getInstance() {
        return ourInstance;
    }

    private GoogleAuthData() {
    }

    // Data
    public static final String[] SCOPES = {CalendarScopes.CALENDAR};

    private GoogleAccountCredential mCredential;

    public void setupCredentials(Context ctx) {
        mCredential = GoogleAccountCredential.usingOAuth2(
                ctx, Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        this.mCalendarService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("UW Calendar")
                .build();
    }

    public void setAccountName(String accountName) {
        if (mCredential.getSelectedAccountName() == null)
            mCredential.setSelectedAccountName(accountName);
    }

    private com.google.api.services.calendar.Calendar mCalendarService = null;

    /**
     * Can only be called after the credentials have been setup.
     */
    public Calendar getService() {
        return mCalendarService;
    }
}
