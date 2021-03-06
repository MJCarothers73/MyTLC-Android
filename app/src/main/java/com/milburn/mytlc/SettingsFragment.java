package com.milburn.mytlc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

public class SettingsFragment extends PreferenceFragment {

    private PrefManager pm;
    private SharedPreferences sharedPref;

    private View primary = null;
    private View accent = null;
    private LinearLayout layoutPrimary;
    private LinearLayout layoutAccent;
    private Credentials credentials;
    private CheckBoxPreference checkPref;
    private CheckBoxPreference checkBackground;
    public ListPreference listCalendars;
    public CheckBoxPreference importCalendar;
    private Activity mActivity;

    private NumberPicker pickerHours;
    private NumberPicker pickerMinute;
    private NumberPicker pickerMinute2;

    private CheckBoxPreference syncAlarm;
    private Preference syncAlarmTime;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
    }
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        credentials = new Credentials(mActivity);

        pm = new PrefManager(mActivity, new PrefManager.onPrefChanged() {
            @Override
            public void prefChanged(SharedPreferences sharedPreferences, String s) {
                setSummary();
            }
        });
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);

        checkPref = (CheckBoxPreference) findPreference(pm.key_past);
        checkBackground = (CheckBoxPreference) findPreference(pm.key_sync_background);

        importCalendar = (CheckBoxPreference) findPreference(pm.key_sync_import);
        listCalendars = (ListPreference) findPreference(pm.key_sync_import_calendar);

        syncAlarm = (CheckBoxPreference) findPreference(pm.key_sync_alarm);
        syncAlarmTime = findPreference(pm.key_sync_alarm_time);

        setSummary();
        setChecked();

        findPreference(pm.key_custom).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showColorPicker();
                return false;
            }
        });

        findPreference(pm.key_sync_alarm_time).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showOffsetPicker();
                return false;
            }
        });

        findPreference(pm.key_past).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!checkPref.isChecked() && sharedPref.contains("PastSchedule")) {
                    showConfirmation();
                }
                return false;
            }
        });

        findPreference(pm.key_sync_background).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (checkBackground.isChecked()) {
                    pm.changeAlarm(1);
                } else {
                    pm.changeAlarm(0);
                }
                return false;
            }
        });

        findPreference(pm.key_sync_import).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (importCalendar.isChecked() && !checkPerms()) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR}, 0);
                }
                setChecked();
                return false;
            }
        });
    }

    private void showColorPicker() {
        View v  = mActivity.getLayoutInflater().inflate(R.layout.dialog_color, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setView(v);
        builder.setTitle("Custom colors");
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (primary != null && accent != null) {
                    pm.setTheme(primary, accent);
                }
            }
        });
        builder.create();
        builder.show();

        layoutPrimary = v.findViewById(R.id.primaryColors);
        layoutAccent = v.findViewById(R.id.accentColors);

        for (int i = 0; i < layoutPrimary.getChildCount(); i++) {
            View pv = layoutPrimary.getChildAt(i);
            if (getSavedColors()[0].equals(pv.getId())) {
                changeSelectedColor(pv);
            }
            pv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeSelectedColor(view);
                }
            });
        }

        for (int i = 0; i < layoutAccent.getChildCount(); i++) {
            View av = layoutAccent.getChildAt(i);
            if (getSavedColors()[1].equals(av.getId())) {
                changeSelectedColor(av);
            }
            av.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeSelectedColor(view);
                }
            });
        }
    }

    private void showOffsetPicker() {
        View v  = mActivity.getLayoutInflater().inflate(R.layout.dialog_alarm_offset, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setView(v);
        builder.setTitle("Alarm offset");
        builder.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                pm.setSyncAlarmTime(pickerHours.getValue()+":"+pickerMinute.getValue());
            }
        });
        builder.create();
        builder.show();

        String[] pickerValues = pm.getSyncAlarmTime().split(":");
        pickerHours = v.findViewById(R.id.numberPicker_Hours);
        pickerMinute = v.findViewById(R.id.numberPicker_Minute);
        pickerHours.setMaxValue(12);
        pickerMinute.setMaxValue(59);

        pickerHours.setValue(Integer.valueOf(pickerValues[0]));
        pickerMinute.setValue(Integer.valueOf(pickerValues[1]));
    }

    private void setSummary() {
        findPreference(pm.key_pay).setSummary("$" + pm.getPay());
        findPreference(pm.key_tax).setSummary(pm.getTax() + "%");
        findPreference(pm.key_base).setSummary(pm.getBase());
        findPreference(pm.key_sync_import_calendar).setSummary(pm.getSelectedCalendar());
        findPreference(pm.key_sync_alarm_time).setSummary("-"+pm.getSyncAlarmTime()+" hours");
        syncAlarmTime.setEnabled(syncAlarm.isChecked());
    }

    private void changeSelectedColor(View view) {
        int height_selected = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        int height_normal = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams params;
        LinearLayout layout = (LinearLayout) view.getParent();
        if (layout.getId() == R.id.primaryColors) {
            if (primary != null) {
                params = primary.getLayoutParams();
                params.height = height_normal;
                primary.setLayoutParams(params);
            }
            primary = view;
            params = primary.getLayoutParams();
            params.height = height_selected;
            primary.setLayoutParams(params);
        } else if (layout.getId() == R.id.accentColors) {
            if (accent != null) {
                params = accent.getLayoutParams();
                params.height = height_normal;
                accent.setLayoutParams(params);
            }
            accent = view;
            params = accent.getLayoutParams();
            params.height = height_selected;
            accent.setLayoutParams(params);
        }
    }

    private Integer[] getSavedColors() {
        Integer primaryId = mActivity.getResources().getIdentifier("id/" + pm.getPrimary(), null, mActivity.getPackageName());
        Integer accentId = mActivity.getResources().getIdentifier("id/" + pm.getAccent() + "_A", null, mActivity.getPackageName());

        return new Integer[]{primaryId, accentId};
    }

    private void showConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Are you sure?");
        builder.setMessage("Disabling shift archiving will clear all currently stored past shifts.");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                credentials.clearPastSchedule();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                pm.setPast(true);
                checkPref.setChecked(true);
            }
        });
        builder.create();
        builder.show();
    }

    private void setChecked() {
        if (importCalendar.isChecked() && checkPerms()) {
            CalendarHelper calendarHelper = new CalendarHelper(mActivity);
            CharSequence[] calNames = calendarHelper.getCalendarNames();
            listCalendars.setEntries(calNames);
            listCalendars.setEntryValues(calNames);
            listCalendars.setDefaultValue(calNames[0]);

            importCalendar.setChecked(true);
            listCalendars.setEnabled(true);
        } else if (!importCalendar.isChecked() || !checkPerms()){
            importCalendar.setChecked(false);
            listCalendars.setEnabled(false);
        }
    }

    private boolean checkPerms() {
        return ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }
}