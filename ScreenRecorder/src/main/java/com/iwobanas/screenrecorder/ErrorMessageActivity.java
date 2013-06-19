package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ErrorMessageActivity extends Activity {
    public static final String ERROR_MESSAGE_EXTRA = "ERROR_MESSAGE_EXTRA";
    public static final String ERROR_TITLE_EXTRA = "ERROR_TITLE_EXTRA";
    public static final String RESTART_EXTRA = "RESTART_EXTRA";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogFragment dialogFragment = new DialogFragment();
        dialogFragment.show(getFragmentManager(), "errorDialog");
    }

    static class DialogFragment extends android.app.DialogFragment {

        private boolean restart;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            Intent intent = activity.getIntent();
            builder.setMessage(intent.getStringExtra(ERROR_MESSAGE_EXTRA));
            builder.setTitle(intent.getStringExtra(ERROR_TITLE_EXTRA));
            builder.setIcon(R.drawable.ic_launcher);
            restart = intent.getBooleanExtra(RESTART_EXTRA, false);
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Activity activity = getActivity();
            if (restart) {
                Intent intent = new Intent(activity, RecorderService.class);
                activity.startService(intent);
            }
            activity.finish();
        }
    }
}


