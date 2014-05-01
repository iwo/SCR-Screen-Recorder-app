package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.DialogActivity;
import com.iwobanas.screenrecorder.R;

import static com.iwobanas.screenrecorder.Tracker.BUY_ERROR;
import static com.iwobanas.screenrecorder.Tracker.ERROR;
import static com.iwobanas.screenrecorder.Tracker.WATERMARK_DIALOG;

public class HideIconDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.hide_icon_message));
        builder.setTitle(getString(R.string.hide_icon_title));
        builder.setIcon(R.drawable.ic_launcher);

        builder.setPositiveButton(getString(R.string.free_timeout_buy), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Activity activity = getActivity();
                if (activity == null)
                    return;

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=com.iwobanas.screenrecorder.pro&referrer=utm_source%3Ddialog%26utm_campaign%3Dhide_icon"));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    EasyTracker.getTracker().sendEvent(ERROR, BUY_ERROR, WATERMARK_DIALOG, null);

                    showErrorDialog(activity);
                }
            }
        });

        builder.setNegativeButton(getString(R.string.free_timeout_no_thanks), null);
        return builder.create();
    }

    private void showErrorDialog(Context context) {
        Intent errorIntent = new Intent(context, DialogActivity.class);
        errorIntent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.buy_error_message));
        errorIntent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.buy_error_title));
        errorIntent.putExtra(DialogActivity.RESTART_EXTRA, false);
        errorIntent.putExtra(DialogActivity.REPORT_BUG_EXTRA, false);
        startActivity(errorIntent);
    }
}
