package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;

public class SettingsActivity extends Activity {
    public static final String TAG = "SettingsActivity";

    private SettingsDialogFragment dialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialogFragment = new SettingsDialogFragment();
        dialogFragment.show(getFragmentManager(), "settingsDialog");
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent(this, RecorderService.class);
        intent.putExtra(RecorderService.SETTINGS_CLOSED_EXTRA, true);
        startService(intent);
        super.onDestroy();
    }

    public static class SettingsDialogFragment extends android.app.DialogFragment {

        private static int instances = 0;

        private int instance = instances++;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Log.d(TAG, "SettingsDialogFragment.onCreateDialog " + instance);
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
            AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.settings_title);
            String[] items = new String[] {
                    getString(R.string.settings_audio),
                    getString(R.string.settings_resolution),
                    getString(R.string.settings_frame_rate)
            };
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Log.d(TAG, "SettingsDialogFragment.onDismiss " + instance);
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }
}


