package com.example.virtual_fridge;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // initializing views
        View view = inflater.inflate(R.layout.settings_fragment, container, false);

        SwitchMaterial oneDayNotificationSwitch = (SwitchMaterial) view.findViewById(R.id.one_day_notif_switch);
        SwitchMaterial twoDayNotificationSwitch = (SwitchMaterial) view.findViewById(R.id.two_day_notif_switch);
        SwitchMaterial oneWeekNotificationSwitch = (SwitchMaterial) view.findViewById(R.id.one_week_notif_switch);

        Button timeInputButton = view.findViewById(R.id.time_set_button);
        ImageButton timeHelpButton = view.findViewById(R.id.time_set_help_button);

        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.unit_options_group);

        // setting up shared preferences
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        // For notification settings
        if (sharedPref.getBoolean(getString(R.string.one_day_notifs_active), Boolean.parseBoolean(getString(R.string.one_day_notifs_active_def)))) {
            oneDayNotificationSwitch.setChecked(true);
        }
        if (sharedPref.getBoolean(getString(R.string.two_day_notifs_active), Boolean.parseBoolean(getString(R.string.two_day_notifs_active_def)))) {
            twoDayNotificationSwitch.setChecked(true);
        }
        if (sharedPref.getBoolean(getString(R.string.one_week_notifs_active), Boolean.parseBoolean(getString(R.string.one_week_notifs_active_def)))) {
            oneWeekNotificationSwitch.setChecked(true);
        }

        oneDayNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(getString(R.string.one_day_notifs_active), isChecked);
            editor.apply();
        });
        twoDayNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(getString(R.string.two_day_notifs_active), isChecked);
            editor.apply();
        });
        oneWeekNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean(getString(R.string.one_week_notifs_active), isChecked);
            editor.apply();
        });

        // dialog to choose the notification time
        // on click -> set time
        TimePickerDialog timePickerDialog = new TimePickerDialog(timeInputButton.getContext(), (view1, hourOfDay, minute) -> {
            editor.putInt(getString(R.string.notif_hour), hourOfDay);
            editor.putInt(getString(R.string.notif_minute), minute);
            editor.apply();

            if (minute < 10) {
                timeInputButton.setText(hourOfDay + ":0" + minute);
            } else {
                timeInputButton.setText(hourOfDay + ":" + minute);
            }

        }, sharedPref.getInt(getString(R.string.notif_hour), Integer.parseInt(getString(R.string.notif_hour_def))), sharedPref.getInt(getString(R.string.notif_minute), Integer.parseInt(getString(R.string.notif_minute_def))), true);

        // on click -> show dialog
        timeInputButton.setOnClickListener(v -> timePickerDialog.show());

        // help dialog
        // on click -> close dialog
        MaterialAlertDialogBuilder timeHelpDialog = new MaterialAlertDialogBuilder(timeHelpButton.getContext())
                .setTitle("Expiration Check Time")
                .setMessage("Virtual Fridge will check all your items at this time and alert you if any are expiring soon.\n\nAllow notifications to use.")
                .setNegativeButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.query_help_icon)
                .setCancelable(true);

        // on click -> show help dialog
        timeHelpButton.setOnClickListener(v -> timeHelpDialog.create().show());

        if (sharedPref.getInt(getString(R.string.notif_minute), Integer.parseInt(getString(R.string.notif_minute_def))) < 10) {
            timeInputButton.setText(sharedPref.getInt(getString(R.string.notif_hour), Integer.parseInt(getString(R.string.notif_hour_def))) + ":0" + sharedPref.getInt(getString(R.string.notif_minute), Integer.parseInt(getString(R.string.notif_minute_def))));
        } else {
            timeInputButton.setText(sharedPref.getInt(getString(R.string.notif_hour), Integer.parseInt(getString(R.string.notif_hour_def))) + ":" + sharedPref.getInt(getString(R.string.notif_minute), Integer.parseInt(getString(R.string.notif_minute_def))));
        }

        // For unit settings
        if (sharedPref.getBoolean(getString(R.string.unit_is_imperial), Boolean.parseBoolean(getString(R.string.unit_is_imperial_def)))) {
            radioGroup.check(R.id.unit_option_imperial);
        } else {
            radioGroup.check(R.id.unit_option_metric);
        }

        // on click -> apply settings to shared preferences
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.unit_option_imperial) {
                editor.putBoolean(getString(R.string.unit_is_imperial), true);
                editor.apply();
            } else if (checkedId == R.id.unit_option_metric) {
                editor.putBoolean(getString(R.string.unit_is_imperial), false);
                editor.apply();
            }
        });

        return view;
    }
}
