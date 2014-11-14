package com.iwobanas.screenrecorder;

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
import android.view.ContextThemeWrapper;
import android.widget.Toast;

public class LollipopDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        final Context contextThemeWrapper = new ContextThemeWrapper(activity, android.R.style.Theme_Holo);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);

        builder.setTitle(R.string.lollipop_dialog_title);
        builder.setMessage(R.string.lollipop_dialog_message);
        builder.setPositiveButton(R.string.lollipop_dialog_try_now, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                tryNow();
            }
        });
        builder.setNegativeButton(R.string.rating_no_thanks, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startService();
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        startService();
    }

    private void startService() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent intent = new Intent(activity, RecorderService.class);
            intent.setAction(RecorderService.LOUNCHER_ACTION);
            activity.startService(intent);
        }
    }

    private void tryNow() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.iwobanas.screenrecorder.noroot.free&referrer=utm_source%3Ddialog%26utm_campaign%3Dlollipop_root"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getActivity(), R.string.rating_play_error, Toast.LENGTH_LONG).show();
        }
    }
}
