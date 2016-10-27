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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sarangjoshi.uwcalendar.data.FirebaseData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * TODO: add class comment
 *
 * @author Sarang Joshi
 */
public class RequestScheduleFragment extends DialogFragment {

    NameSelectedListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (NameSelectedListener) activity;
        } catch (ClassCastException e) {
            // TODO: Feq!
            throw new ClassCastException("Failed casting");
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        FirebaseData fb = FirebaseData.getInstance();

        List<FirebaseData.UsernameAndId> objects = new ArrayList<>(fb.getAllUsers());

        Iterator<FirebaseData.UsernameAndId> iter = objects.iterator();
        while (iter.hasNext()) {
            FirebaseData.UsernameAndId curr = iter.next();
            if (curr.id.equals(fb.getUid())) {
                iter.remove();
                break;
            }
        }

        ArrayAdapter<FirebaseData.UsernameAndId> adapter = new ArrayAdapter<FirebaseData.UsernameAndId>(getActivity(),
                android.R.layout.select_dialog_singlechoice, objects) {
            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View v = inflater.inflate(android.R.layout.select_dialog_item, null);

                ((TextView) v.findViewById(android.R.id.text1)).setText(getItem(pos).username);

                return v;
            }
        };

        builder.setTitle("Request schedule.")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    List<FirebaseData.UsernameAndId> objects;

                    public DialogInterface.OnClickListener init(List<FirebaseData.UsernameAndId> objects) {
                        this.objects = objects;
                        return this;
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.usernameToRequestSelected(objects.get(which));
                    }
                }.init(objects));
        return builder.create();
    }

    public interface NameSelectedListener {
        void usernameToRequestSelected(FirebaseData.UsernameAndId selected);
    }

}
