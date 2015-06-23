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

public class SystemUICrashDialogFragment extends DialogFragment {

    public final static String FRAGMENT_TAG = "SystemUICrashDialog";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        final Context contextThemeWrapper = new ContextThemeWrapper(activity, android.R.style.Theme_DeviceDefault);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);

        builder.setTitle(R.string.app_name);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setMessage(getString(R.string.system_ui_crash_dialog_message, getString(R.string.app_name), getString(R.string.media_projection_remember_text)));
        builder.setNegativeButton(R.string.settings_ok,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestMediaProjection();
            }
        });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    private void requestMediaProjection() {
        MediaProjectionActivity activity = (MediaProjectionActivity) getActivity();
        if (activity != null) {
            activity.requestMediaProjection();
        }
    }
}
