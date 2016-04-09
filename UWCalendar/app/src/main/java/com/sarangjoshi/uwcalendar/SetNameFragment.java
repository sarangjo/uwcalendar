package com.sarangjoshi.uwcalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.widget.EditText;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class SetNameFragment extends DialogFragment {
    public interface SetNameListener {
        void onSignupClick(String name);
    }

    SetNameListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SetNameListener) activity;
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

        final EditText name = (EditText) inflater.inflate(R.layout.dialog_name, null);

        builder.setView(name)
                .setTitle("Set name.")
                .setPositiveButton(R.string.action_sign_up, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: make sure name is OK
                        mListener.onSignupClick(name.getText().toString());
                    }
                });
        return builder.create();
    }
}
