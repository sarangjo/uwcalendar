package com.sarangjoshi.uwcalendar.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.sarangjoshi.uwcalendar.R;

/**
 * A fragment to allow a user in the process of signing up to set their username.
 *
 * @author Sarang Joshi
 */
public class SetUsernameFragment extends DialogFragment {
    public interface SetUsernameListener {
        void attemptSignup(String name);
    }

    SetUsernameListener mListener;

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SetUsernameListener) context;
        } catch (ClassCastException e) {
            // TODO: Feq!
            throw new ClassCastException("Failed casting");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final EditText username = (EditText) inflater.inflate(R.layout.dialog_username, null);

        builder.setView(username)
                .setTitle("Set username.")
                .setPositiveButton(R.string.action_sign_up, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: make sure username is OK
                        mListener.attemptSignup(username.getText().toString());
                    }
                });
        return builder.create();
    }
}
