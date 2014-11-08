package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.iwobanas.screenrecorder.settings.Settings;

public class RecorderActivity extends Activity {

    private final String PREFERENCES_NAME = "RecorderActivity";
    private final String LOLLIPOP_MESSAGE = "lollipop_message";
    private final String LOLLIPOP_DIALOG_TAG = "LollipopDialog";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT == 21 && !getSharedPreferences(PREFERENCES_NAME, 0).getBoolean(LOLLIPOP_MESSAGE, false)) {
            getSharedPreferences(PREFERENCES_NAME, 0).edit().putBoolean(LOLLIPOP_MESSAGE, true).apply();
            new LollipopDialogFragment().show(getFragmentManager(), LOLLIPOP_DIALOG_TAG);
        } else if (getFragmentManager().findFragmentByTag(LOLLIPOP_DIALOG_TAG) == null) { // handle configuration changes
            Intent intent = new Intent(this, RecorderService.class);
            intent.setAction(RecorderService.LOUNCHER_ACTION);
            startService(intent);
            finish();
        }
    }
}
