package com.iwobanas.screenrecorder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.ADS;
import static com.iwobanas.screenrecorder.Tracker.GUSHER_DIALOG;

public class GusherDialogFragment extends DialogFragment {

    public static final String FRAGMENT_TAG = "GusherDialog";
    public static final String GUSHER_PACKAGE_ID = "com.smamolot.gusher";
    private static final String PREFERENCES_NAME = "GusherDialog";
    private static final String KEY_HIDE = "hide";
    private static final String KEY_LAST_SHOWN = "last_shown";
    private static final String KEY_SHOW_COUNT = "show_count";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final long MIN_SHOW_INTERVAL_MS = 12 * 60 * 60 * 1000; // 12h
    private static final long INITIAL_DELAY = 30 * 60 * 1000; // 30 min
    private CheckBox rememberCheckBox;
    private boolean positiveSelected;
    private int showCount;

    public static boolean shouldShow(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        try {
            context.getPackageManager().getPackageInfo(GUSHER_PACKAGE_ID, PackageManager.GET_ACTIVITIES);
            return false;
        } catch (PackageManager.NameNotFoundException ignore) {
        }

        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (preferences.contains(KEY_HIDE)) {
            return false;
        }

        long now = System.currentTimeMillis();

        if (!preferences.contains(KEY_FIRST_RUN)) {
            preferences.edit().putLong(KEY_FIRST_RUN, now).apply();
            return false;
        }

        long firstRun = preferences.getLong(KEY_FIRST_RUN, 0);
        long lastShown = preferences.getLong(KEY_LAST_SHOWN, 0);

        return (now - firstRun) > INITIAL_DELAY && (now - lastShown) > MIN_SHOW_INTERVAL_MS;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        showCount = preferences.getInt(KEY_SHOW_COUNT, 0);
        showCount++;
        preferences.edit()
                .putInt(KEY_SHOW_COUNT, showCount)
                .putLong(KEY_LAST_SHOWN, System.currentTimeMillis())
                .apply();

        final Context contextThemeWrapper = new ContextThemeWrapper(activity, android.R.style.Theme_Material);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);

        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(activity.getString(R.string.gusher_dialog_title));
        builder.setMessage(activity.getString(R.string.gusher_dialog_message));
        if (showCount > 3) {
            LayoutInflater layoutInflater = LayoutInflater.from(contextThemeWrapper);
            View rememberView = layoutInflater.inflate(R.layout.remember_checkbox, null, false);
            rememberCheckBox = (CheckBox) rememberView.findViewById(R.id.remember);
            builder.setView(rememberView);
        }
        builder.setPositiveButton(activity.getString(R.string.gusher_dialog_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                positiveSelected = true;
                setHideFlag();
                openGooglePlay();
            }
        });
        builder.setNegativeButton(activity.getString(R.string.gusher_dialog_negative), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (rememberCheckBox != null && rememberCheckBox.isChecked()) {
                    setHideFlag();
                }
            }
        });

        return builder.create();
    }

    private void openGooglePlay() {
        Activity activity = getActivity();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + GUSHER_PACKAGE_ID + "&referrer=utm_source%3DSCR%26utm_medium%3Ddialog%26utm_campaign%3Ddialog"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.rating_play_error, Toast.LENGTH_LONG).show();
        }
    }

    private void setHideFlag() {
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
            preferences.edit().putBoolean(KEY_HIDE, true).apply();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity != null) {
            if (!positiveSelected) {
                Intent intent = new Intent(activity, RecorderService.class);
                intent.setAction(RecorderService.LOUNCHER_ACTION);
                activity.startService(intent);
            }
            activity.finish();
            EasyTracker.getInstance().setContext(activity.getApplicationContext());
            EasyTracker.getTracker().sendEvent(ADS, GUSHER_DIALOG, GUSHER_DIALOG + showCount, positiveSelected ? 1l : 0l);
        }
    }
}
