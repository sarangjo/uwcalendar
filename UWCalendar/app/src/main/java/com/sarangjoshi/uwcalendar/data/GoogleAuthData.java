package com.sarangjoshi.uwcalendar.data;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
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

    private GoogleSignInAccount mAccount;

    public static GoogleAuthData getInstance() {
        return ourInstance;
    }

    private GoogleAuthData() {
    }

    public void setAccount(GoogleSignInAccount acct) {
        this.mAccount = acct;
    }

    public GoogleSignInAccount getAccount() {
        return this.mAccount;
    }
}