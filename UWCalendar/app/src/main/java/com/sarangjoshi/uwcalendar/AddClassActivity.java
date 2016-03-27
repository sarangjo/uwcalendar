package com.sarangjoshi.uwcalendar;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;

import java.util.Calendar;

public class AddClassActivity extends AppCompatActivity {
    FirebaseData mFirebaseData;

    Button mStartTimePicker, mEndTimePicker;
    EditText mClassName, mClassLocation;
    CheckBox[] mCheckboxes;

    String mStartTime, mEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mStartTimePicker = (Button) findViewById(R.id.start_time_picker);
        mEndTimePicker = (Button) findViewById(R.id.end_time_picker);
        mClassName = (EditText) findViewById(R.id.class_name);
        mClassLocation = (EditText) findViewById(R.id.class_location);

        mCheckboxes = new CheckBox[5];
        mCheckboxes[0] = (CheckBox) findViewById(R.id.mondayCheckBox);
        mCheckboxes[1] = (CheckBox) findViewById(R.id.tuesdayCheckBox);
        mCheckboxes[2] = (CheckBox) findViewById(R.id.wednesdayCheckBox);
        mCheckboxes[3] = (CheckBox) findViewById(R.id.thursdayCheckBox);
        mCheckboxes[4] = (CheckBox) findViewById(R.id.fridayCheckBox);
    }

    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment(v.getId() == R.id.start_time_picker);
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    int[] dbDaysMap = {16, 8, 4, 2, 1};

    public int getDays() {
        int days = 0;
        for (int i = 0; i < mCheckboxes.length; i++) {
            CheckBox b = mCheckboxes[i];
            if (b.isChecked()) {
                days = days | dbDaysMap[i];
            }
        }
        return days;
    }

    public class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {
        private boolean isStart;

        public TimePickerFragment(boolean isStart) {
            this.isStart = isStart;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            String time = String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute);
            if (isStart) {
                mStartTime = time;
                mStartTimePicker.setText(mStartTime);
            } else {
                mEndTime = time;
                mEndTimePicker.setText(mEndTime);
            }
            dismiss();
        }

    }

    public void addClass(View view) {
        // Pass the data back to the parent
        Intent data = new Intent();

        data.putExtra("name", mClassName.getText().toString());
        data.putExtra("location", mClassLocation.getText().toString());
        data.putExtra("days", getDays());
        data.putExtra("start", mStartTime);
        data.putExtra("end", mEndTime);

        // TODO: update to actually be a quarter
        data.putExtra("quarter", "sp16");

        setResult(RESULT_OK, data);
        finish();
    }
}
