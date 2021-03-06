package com.sarangjoshi.uwcalendar.activities;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
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

import com.sarangjoshi.uwcalendar.R;
import com.sarangjoshi.uwcalendar.SetTimeListener;
import com.sarangjoshi.uwcalendar.singletons.ScheduleData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * TODO: add option to choose color (for Google event)
 */
public class AddClassActivity extends AppCompatActivity implements SetTimeListener {
    public static final String NAME_KEY = "name";
    public static final String LOCATION_KEY = "location";
    public static final String DAYS_KEY = "days";
    public static final String START_KEY = "start";
    public static final String END_KEY = "end";

    private static final String IS_START_KEY = "isStart";
    private static final String TIMES_KEY = "times";

    private Button mStartTimePicker, mEndTimePicker;
    private EditText mClassName, mClassLocation;
    private CheckBox[] mCheckboxes;
    private ListView mErrorText;

    private String[] mTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        DialogFragment newFragment = TimePickerFragment.newInstance(v.getId() == R.id.start_time_picker, mTimes);
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

    public void addClass(View view) {
        // TODO: check for errors
        List<String> errors = checkErrors();
        if (errors.size() != 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, errors);
            mErrorText.setAdapter(adapter);
        } else {
            // Pass the data back to the parent
            Intent data = new Intent();

            data.putExtra(NAME_KEY, mClassName.getText().toString());
            data.putExtra(LOCATION_KEY, mClassLocation.getText().toString());
            data.putExtra(DAYS_KEY, getDays());
            data.putExtra(START_KEY, mTimes[0]);
            data.putExtra(END_KEY, mTimes[1]);

            setResult(RESULT_OK, data);

            finish();
        }
    }

    /**
     * Returns a collection of the errors.
     */
    private List<String> checkErrors() {
        List<String> myErrors = new ArrayList<>();

        if (mClassName.getText().toString().trim().isEmpty()) {
            myErrors.add("Enter a valid class name.");
        }
        if (mClassLocation.getText().toString().trim().isEmpty()) {
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

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {
        private boolean isStart;
        private String[] mTimes;
        private SetTimeListener mListener;

        public TimePickerFragment() {
        }

        public static TimePickerFragment newInstance(boolean isStart, String[] times) {
            TimePickerFragment frag = new TimePickerFragment();
            Bundle b = new Bundle();
            b.putBoolean(IS_START_KEY, isStart);
            b.putStringArray(TIMES_KEY, times);
            frag.setArguments(b);
            return frag;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mListener = (SetTimeListener) context;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get arguments from bundle
            Bundle args = getArguments();
            isStart = args.getBoolean(IS_START_KEY);
            mTimes = args.getStringArray(TIMES_KEY);

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

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mListener.onTimeSet(isStart, hourOfDay, minute);
            dismiss();
        }
    }

    public void onTimeSet(boolean isStart, int hourOfDay, int minute) {
        // TODO: limit start times? earliest // latest
        String time = String.format("%02d:%02d", hourOfDay, minute);
        if (isStart) {
            mTimes[0] = time;
            mStartTimePicker.setText(mTimes[0]);

            mTimes[1] = getEndTime(hourOfDay, minute);
            mEndTimePicker.setText(mTimes[1]);
        } else {
            mTimes[1] = time;
            mEndTimePicker.setText(mTimes[1]);
        }
    }

    /**
     * Gets the end time given start hour and minute.
     */
    private String getEndTime(int startHourOfDay, int startMinute) {
        startMinute -= 10;
        if (startMinute >= 0) {
            startHourOfDay += 1;
        } else {
            startMinute += 60;
        }
        return String.format("%02d:%02d", startHourOfDay, startMinute);
    }
}
