package com.iwobanas.screenrecorder.rating;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.RecorderService;

import static com.iwobanas.screenrecorder.Tracker.RATING;
import static com.iwobanas.screenrecorder.Tracker.RATING_CANCEL;
import static com.iwobanas.screenrecorder.Tracker.RATING_NO_THANKS;
import static com.iwobanas.screenrecorder.Tracker.RATING_RATE_NOW;
import static com.iwobanas.screenrecorder.Tracker.RATING_REMIND;
import static com.iwobanas.screenrecorder.Tracker.RATING_SHOW;

public class RatingDialogFragment extends DialogFragment {

    private SharedPreferences preferences;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (preferences == null) {
            preferences = activity.getSharedPreferences(RatingController.PREFERENCES, Context.MODE_PRIVATE);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        final Context contextThemeWrapper = new ContextThemeWrapper(activity, android.R.style.Theme_Holo);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);

        builder.setTitle(String.format(getString(R.string.rating_rate_title), getString(R.string.app_name)));
        builder.setMessage(String.format(getString(R.string.rating_rate_message), getString(R.string.app_name)));

        builder.setPositiveButton(R.string.rating_rate_now, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                rateNow();
            }
        });

        builder.setNeutralButton(R.string.rating_remind, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                remindLater();
            }
        });

        builder.setNegativeButton(R.string.rating_no_thanks, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                noThanks();
            }
        });

        EasyTracker.getTracker().sendEvent(RATING, RATING_SHOW, RATING_SHOW, null);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        EasyTracker.getTracker().sendEvent(RATING, RATING_CANCEL, RATING_CANCEL, null);
        goBackToOverlay();
    }

    private void rateNow() {
        Activity activity = getActivity();
        if (activity == null || preferences == null) return;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + activity.getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.rating_play_error, Toast.LENGTH_LONG).show();
            goBackToOverlay();
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(RatingController.DISABLED, true);
        editor.commit();
        EasyTracker.getTracker().sendEvent(RATING, RATING_RATE_NOW, RATING_RATE_NOW, null);
    }

    private void remindLater() {
        EasyTracker.getTracker().sendEvent(RATING, RATING_REMIND, RATING_REMIND, null);
        goBackToOverlay();
    }

    private void noThanks() {
        if (preferences == null) return;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(RatingController.DISABLED, true);
        editor.commit();
        EasyTracker.getTracker().sendEvent(RATING, RATING_NO_THANKS, RATING_NO_THANKS, null);
        goBackToOverlay();
    }

    private void goBackToOverlay() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, RecorderService.class);
            activity.startService(intent);
            activity.finish();
        }
    }
}
