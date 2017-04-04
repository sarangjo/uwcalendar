package com.sarangjoshi.uwcalendar.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.sarangjoshi.uwcalendar.activities.HomeActivity;

/**
 * TODO: Write a class comment
 */

public class AcceptRequestDialogFragment extends DialogFragment {
    public static final String ACCEPT_REQUEST_POSITION = "acceptRequestPosition";

    private AcceptRequestListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (AcceptRequestListener) context;
        } catch (ClassCastException e) {
            // TODO: Feq!
            throw new ClassCastException("Failed casting");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final int position = getArguments().getInt(ACCEPT_REQUEST_POSITION);

        // Setup "accept decline" dialog
        builder.setTitle("Accept request.").setMessage("Accept request?").setPositiveButton(" Accept", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.accept(position);
            }
        }).setNegativeButton("Decline", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.decline(position);
            }
        });

        return builder.create();
    }

    public interface AcceptRequestListener {
        void accept(int position);
        void decline(int position);
    }
}
