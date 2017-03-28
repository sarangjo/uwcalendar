package com.sarangjoshi.uwcalendar.singletons;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Arrays;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class GoogleAuth {
    //public static final boolean GOOGLE_ENABLED = false;

    public static final String ACCOUNT_NAME_KEY = "accountName"; // TODO: clear this on logout
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    public static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    private GoogleAccountCredential mCredential;

    private static GoogleAuth ourInstance = new GoogleAuth();

    private GoogleSignInAccount mAccount;

    public static GoogleAuth getInstance() {
        return ourInstance;
    }

    private GoogleAuth() {

    }

    public void initializeCredential(Context appContext) {
        // Initialize Google auth credential object
        mCredential = GoogleAccountCredential.usingOAuth2(appContext, Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    public boolean isGooglePlayServicesAvailable(Context activity) {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(activity);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    public String getSelectedAccountName() {
        return mCredential.getSelectedAccountName();
    }

    public void setSelectedAccountName(String accountName) {
        mCredential.setSelectedAccountName(accountName);
    }

    public Intent chooseAccount() {
        return mCredential.newChooseAccountIntent();
    }

    public Dialog playServicesAvailabilityError(Activity activity, int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        return apiAvailability.getErrorDialog(
                activity,
                connectionStatusCode,
                GoogleAuth.REQUEST_GOOGLE_PLAY_SERVICES);
    }

    public GoogleAccountCredential getCredential() {
        return mCredential;
    }
}