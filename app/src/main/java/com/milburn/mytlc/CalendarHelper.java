package com.milburn.mytlc;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CalendarHelper extends AsyncTask<List<Shift>, Integer, Void> {

    private Context context;
    private List<Shift> shiftList;
    private Credentials credentials;
    private ContentResolver cr;
    private Uri calUri;
    private Uri eventUri;

    private Spinner spinnerCalendar = null;
    private EditText editEventTitle = null;
    private EditText editAddress = null;
    private EditText editStore = null;
    private Button buttonStore = null;
    private CheckBox checkDelete = null;

    private AlertDialog.Builder builder;
    private Snackbar snackBar;
    private String snackString;
    private View dialogView;
    private CharSequence[] calendarNames;
    private HashMap<Integer, String> calendarMap;

    public CalendarHelper(Context con) {
        context = con;
        credentials = new Credentials(context);

        cr = context.getContentResolver();
        calUri = CalendarContract.Calendars.CONTENT_URI;
        eventUri = CalendarContract.Events.CONTENT_URI;
    }

    @Override
    protected Void doInBackground(List<Shift>... params) {
        shiftList = params[0];
        importToCalendar();
        return null;
    }

    private void importToCalendar() {
        Cursor cur = null;

        final String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };

        try {
            cur = cr.query(calUri, EVENT_PROJECTION, null, null, null);
        } catch (SecurityException se) {
            se.printStackTrace();
        }

        calendarMap = new HashMap<>();
        while (cur.moveToNext()) {
            calendarMap.put(cur.getInt(0), cur.getString(1));
        }
        cur.close();
        calendarNames = calendarMap.values().toArray(new CharSequence[calendarMap.size()]);
        if (calendarNames.length < 1) {
            snackString = "No calendars available";
            publishProgress(1);
            return;
        }

        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_import, null);

        builder = new AlertDialog.Builder(context);
        builder.setTitle("Customize your events")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Import", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String calName = calendarMap.get(spinnerCalendar.getSelectedItemPosition());

                        if (checkDelete.isChecked()) {
                            deleteEvents(calName);
                        }

                        List<Long> eventIds = new ArrayList<>();
                        for (Shift shift : shiftList) {
                            ContentValues values = new ContentValues();
                            values.put(CalendarContract.Events.DTSTART, shift.getStartTime().getTime());
                            values.put(CalendarContract.Events.DTEND, shift.getEndTime().getTime());
                            values.put(CalendarContract.Events.TITLE, editEventTitle.getText().toString());
                            values.put(CalendarContract.Events.DESCRIPTION, "Departments: " + shift.getCombinedDepts());
                            values.put(CalendarContract.Events.CALENDAR_ID, spinnerCalendar.getSelectedItemPosition());
                            values.put(CalendarContract.Events.EVENT_TIMEZONE, java.util.Calendar.getInstance().getTimeZone().getDisplayName());
                            values.put(CalendarContract.Events.EVENT_LOCATION, editAddress.getText().toString());

                            try {
                                Uri uri = cr.insert(eventUri, values);
                                Long id = Long.parseLong(uri.getLastPathSegment());
                                eventIds.add(id);
                            } catch (SecurityException se) {
                                se.printStackTrace();
                            }
                        }
                        if (!eventIds.isEmpty()) {
                            credentials.addEventIds(calName, eventIds);
                        }
                        snackString = "Events successfully added to " + "'" + calName + "'";
                        publishProgress(1);
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                });
        publishProgress(2);
    }

    public void deleteEvents(String calName) {
        if (credentials.getEventIds(calName) != null) {
            List<Long> eventIds = credentials.getEventIds(calName);
            for (Long id : eventIds) {
                Uri deleteUri = ContentUris.withAppendedId(eventUri, id);
                cr.delete(deleteUri, null, null);
            }
            credentials.removeEventIds(calName);
        }
    }

    public void deleteEvents() {
        if (!credentials.getEventIds().isEmpty()) {
            for (List<Long> listIds : credentials.getEventIds().values()) {
                for (Long id : listIds) {
                    Uri deleteUri = ContentUris.withAppendedId(eventUri, id);
                    cr.delete(deleteUri, null, null);
                }
            }
            credentials.setEventIds(new HashMap<String, List<Long>>());
        }
    }

    private void getStoreAddress() {
        BBYApi bbyApi = new BBYApi(context, new BBYApi.AsyncResponse() {
            @Override
            public void processFinish(String address) {
                if (address != null) {
                    editAddress.setText(address);
                }
            }
        });

        String storeId = editStore.getText().toString();
        if (!storeId.isEmpty()) {
            bbyApi.execute(storeId);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        switch (progress[0]) {
            case 1:
                snackBar = Snackbar.make(((Activity) context).findViewById(R.id.coordinatorLayout), snackString, Snackbar.LENGTH_LONG);
                snackBar.show();
                break;

            case 2:
                builder.create();
                builder.show();

                spinnerCalendar = (Spinner) dialogView.findViewById(R.id.spinner_calendar);
                editEventTitle = (EditText) dialogView.findViewById(R.id.edit_title);
                editAddress = (EditText) dialogView.findViewById(R.id.edit_store_address);
                editStore = (EditText) dialogView.findViewById(R.id.edit_store_number);
                buttonStore = (Button) dialogView.findViewById(R.id.button_store);

                buttonStore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getStoreAddress();
                    }
                });
                editStore.setText(shiftList.get(0).getStoreNumber());
                getStoreAddress();

                checkDelete = (CheckBox) dialogView.findViewById(R.id.check_delete);
                checkDelete.setChecked(true);

                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, calendarNames);
                spinnerCalendar.setAdapter(adapter);

                if (credentials.getLastCalName(calendarMap) != null) {
                    Object[] calNamePos = credentials.getLastCalName(calendarMap);
                    String calName = (String)calNamePos[0];
                    Integer calNameIndex = (Integer)calNamePos[1];

                    spinnerCalendar.setSelection(calNameIndex);
                }
                break;
        }
    }
}