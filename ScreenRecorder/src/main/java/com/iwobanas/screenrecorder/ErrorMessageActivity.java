package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ErrorMessageActivity extends Activity {
    public static final String ERROR_MESSAGE_EXTRA = "ERROR_MESSAGE_EXTRA";
    public static final String RESTART_EXTRA = "RESTART_EXTRA";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogFragment dialogFragment = new DialogFragment();
        dialogFragment.show(getFragmentManager(), "errorDialog");
    }

    class DialogFragment extends android.app.DialogFragment {

        private boolean restart;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ErrorMessageActivity.this);
            Intent intent = getIntent();
            builder.setMessage(intent.getStringExtra(ERROR_MESSAGE_EXTRA));
            restart = intent.getBooleanExtra(RESTART_EXTRA, false);
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (restart) {
                Intent intent = new Intent(ErrorMessageActivity.this, RecorderService.class);
                startService(intent);
            }
            finish();
        }
    }
}


