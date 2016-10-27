package com.sarangjoshi.uwcalendar.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.sarangjoshi.uwcalendar.R;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class ChangePasswordFragment extends DialogFragment {
    ChangePasswordListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ChangePasswordListener) activity;
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

        View v = inflater.inflate(R.layout.dialog_change_password, null);
        final EditText oldPassText = (EditText) v.findViewById(R.id.old_password);
        final EditText newPassText = (EditText) v.findViewById(R.id.new_password);

        builder.setTitle("Change password.")
                .setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: basic verification?
                        mListener.passwordChanged(oldPassText.getText().toString(), newPassText.getText().toString());
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        return builder.create();
    }

    public interface ChangePasswordListener {
        void passwordChanged(String oldPass, String newPass);
    }
}
