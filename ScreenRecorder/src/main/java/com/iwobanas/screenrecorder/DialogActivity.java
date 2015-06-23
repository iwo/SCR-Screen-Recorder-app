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

import com.iwobanas.screenrecorder.settings.Settings;

public class DialogActivity extends Activity {
    private static final String TAG = "scr_DialogActivity";
    public static final String MESSAGE_EXTRA = "MESSAGE_EXTRA";
    public static final String TITLE_EXTRA = "TITLE_EXTRA";
    public static final String RESTART_EXTRA = "RESTART_EXTRA";
    public static final String POSITIVE_EXTRA = "POSITIVE_EXTRA";
    public static final String POSITIVE_INTENT_EXTRA = "POSITIVE_INTENT_EXTRA";
    public static final String NEUTRAL_EXTRA = "NEUTRAL_EXTRA";
    public static final String NEUTRAL_INTENT_EXTRA = "NEUTRAL_INTENT_EXTRA";
    public static final String NEGATIVE_EXTRA = "NEGATIVE_EXTRA";
    public static final String NEGATIVE_INTENT_EXTRA = "NEGATIVE_INTENT_EXTRA";
    public static final String RESTART_ACTION_EXTRA = "RESTART_ACTION_EXTRA";
    public static final String REPORT_BUG_EXTRA = "REPORT_BUG_EXTRA";
    public static final String REPORT_BUG_ERROR_EXTRA = "REPORT_BUG_ERROR_EXTRA";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        DialogFragment dialogFragment = new DialogFragment();
        dialogFragment.show(getFragmentManager(), "errorDialog");
    }

    public static class DialogFragment extends android.app.DialogFragment {

        private boolean restart;

        private String restartAction;

        private boolean positiveSelected;

        private boolean neutralSelected;

        private boolean negativeSelected;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_DeviceDefault);
            AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
            Intent intent = activity.getIntent();
            builder.setMessage(intent.getStringExtra(MESSAGE_EXTRA));
            builder.setTitle(intent.getStringExtra(TITLE_EXTRA));
            builder.setIcon(R.drawable.ic_launcher);
            if (Settings.getInstance().isRootEnabled() && intent.getBooleanExtra(REPORT_BUG_EXTRA, false)) {
                final int errorCode = intent.getIntExtra(REPORT_BUG_ERROR_EXTRA, -1);
                builder.setPositiveButton(R.string.error_report_report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new ReportBugTask(getActivity().getApplicationContext(), errorCode).execute();
                    }
                });
                builder.setNegativeButton(R.string.error_report_close, null);
            } else {
                String positiveLabel = intent.getStringExtra(POSITIVE_EXTRA);
                if (positiveLabel != null) {
                    final Intent positiveIntent = intent.getParcelableExtra(POSITIVE_INTENT_EXTRA);
                    builder.setPositiveButton(positiveLabel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            positiveSelected = true;
                            if (positiveIntent != null) {
                                try {
                                    startActivity(positiveIntent);
                                } catch (Exception e) {
                                    Log.w(TAG, "Error starting activity", e);
                                }
                            }
                        }
                    });
                }
                String neutralLabel = intent.getStringExtra(NEUTRAL_EXTRA);
                if (neutralLabel != null) {
                    final Intent neutralIntent = intent.getParcelableExtra(NEUTRAL_INTENT_EXTRA);
                    builder.setNeutralButton(neutralLabel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            neutralSelected = true;
                            if (neutralIntent != null) {
                                try {
                                    startActivity(neutralIntent);
                                } catch (Exception e) {
                                    Log.w(TAG, "Error starting activity", e);
                                }
                            }
                        }
                    });
                }
                String negativeLabel = intent.getStringExtra(NEGATIVE_EXTRA);
                if (negativeLabel != null) {
                    final Intent negativeIntent = intent.getParcelableExtra(NEGATIVE_INTENT_EXTRA);
                    builder.setNegativeButton(negativeLabel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            negativeSelected = true;
                            if (negativeIntent != null) {
                                try {
                                    startActivity(negativeIntent);
                                } catch (Exception e) {
                                    Log.w(TAG, "Error starting activity", e);
                                }
                            }
                        }
                    });
                }

                if (positiveLabel == null && negativeLabel == null) {
                    builder.setNegativeButton(R.string.error_report_close, null);
                }
            }
            restart = intent.getBooleanExtra(RESTART_EXTRA, false);
            restartAction = intent.getStringExtra(RESTART_ACTION_EXTRA);
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Activity activity = getActivity();
            if (activity == null) return;
            if (restart) {
                Intent intent = new Intent(activity, RecorderService.class);
                if (restartAction != null) {
                    intent.setAction(restartAction);
                } else {
                    intent.setAction(RecorderService.DIALOG_CLOSED_ACTION);
                }
                intent.putExtra(POSITIVE_EXTRA, positiveSelected);
                intent.putExtra(NEUTRAL_EXTRA, neutralSelected);
                intent.putExtra(NEGATIVE_EXTRA, negativeSelected);
                activity.startService(intent);
            }
            activity.finish();
        }
    }
}


