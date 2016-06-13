package com.sarangjoshi.uwcalendar;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;

import com.sarangjoshi.uwcalendar.data.ScheduleData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddClassActivity extends AppCompatActivity {

    Button mStartTimePicker, mEndTimePicker;
    EditText mClassName, mClassLocation;
    CheckBox[] mCheckboxes;
    ListView mErrorText;

    String[] mTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null)
            supportActionBar.setDisplayHomeAsUpEnabled(true);

        mTimes = new String[2];
        mTimes[0] = mTimes[1] = "";
        mStartTimePicker = (Button) findViewById(R.id.start_time_picker);
        mEndTimePicker = (Button) findViewById(R.id.end_time_picker);
        mClassName = (EditText) findViewById(R.id.class_name);
        mClassLocation = (EditText) findViewById(R.id.class_location);

        mErrorText = (ListView) findViewById(R.id.error_text);

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

    public int getDays() {
        int days = 0;
        for (int i = 0; i < mCheckboxes.length; i++) {
            CheckBox b = mCheckboxes[i];
            if (b.isChecked()) {
                days = days | ScheduleData.DAYS_MAP[i];
            }
        }
        return days;
    }

    private class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {
        private boolean isStart;

        public TimePickerFragment(boolean isStart) {
            this.isStart = isStart;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int i = isStart ? 0 : 1;
            int hour, minute;

            if (mTimes[i].isEmpty()) {
                // Use the current time as the default values for the picker
                Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            } else {
                hour = Integer.parseInt(mTimes[i].substring(0, 2));
                minute = Integer.parseInt(mTimes[i].substring(3));
            }

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // TODO: limit start times? earliest // latest
            String time = String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute);
            if (isStart) {
                mTimes[0] = time;
                mStartTimePicker.setText(mTimes[0]);

                mTimes[1] = getEndTime(hourOfDay, minute);
                mEndTimePicker.setText(mTimes[1]);
            } else {
                mTimes[1] = time;
                mEndTimePicker.setText(mTimes[1]);
            }
            dismiss();
        }

        private String getEndTime(int hourOfDay, int minute) {
            minute -= 10;
            if (minute >= 0) {
                hourOfDay += 1;
            } else {
                minute += 60;
            }
            return String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute);
        }

    }

    public void addClass(View view) {
        // TODO: check for errors
        List<String> errors = checkErrors();
        if (errors.size() != 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, errors);
            mErrorText.setAdapter(adapter);
        } else {
            // Pass the data back to the parent
            Intent data = new Intent();

            data.putExtra("name", mClassName.getText().toString());
            data.putExtra("location", mClassLocation.getText().toString());
            data.putExtra("days", getDays());
            data.putExtra("start", mTimes[0]);
            data.putExtra("end", mTimes[1]);

            // TODO: update to actually be a quarter
            data.putExtra("quarter", ScheduleData.getInstance().getCurrentQuarter());

            setResult(RESULT_OK, data);

            finish();
        }
    }

    private List<String> checkErrors() {
        List<String> myErrors = new ArrayList<>();

        if (mClassName.getText().toString().equals("")) {
            myErrors.add("Enter a valid class name.");
        }
        if (mClassLocation.getText().toString().equals("")) {
            myErrors.add("Enter a valid class location.");
        }

        boolean timesOK = timesValid();

        if (!timesOK) {
            myErrors.add("Enter valid start/end times.");
        }

        int days = getDays();
        if (days == 0) {
            myErrors.add("Select at least 1 day.");
        }

        return myErrors;
    }

    /**
     * Verifies that start is before end
     * Format 09:30, 21:30
     *
     * @return true if the times are valid
     */
    private boolean timesValid() {
        if (mTimes[0].isEmpty() || mTimes[1].isEmpty()) return false;
        int starthr = Integer.parseInt(mTimes[0].substring(0, 2));
        int endhr = Integer.parseInt(mTimes[1].substring(0, 2));
        return starthr <= endhr && (starthr < endhr || (Integer.parseInt(mTimes[0].substring(3))) < (Integer.parseInt(mTimes[1].substring(3))));
    }

}
