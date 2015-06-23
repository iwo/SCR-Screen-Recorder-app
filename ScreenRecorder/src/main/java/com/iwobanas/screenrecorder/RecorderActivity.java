package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.iwobanas.screenrecorder.settings.Settings;

public class RecorderActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null && GusherDialogFragment.shouldShow(this)) {
            new GusherDialogFragment().show(getFragmentManager(), GusherDialogFragment.FRAGMENT_TAG);
        } else {
            Intent intent = new Intent(this, RecorderService.class);
            intent.setAction(RecorderService.LOUNCHER_ACTION);
            startService(intent);
            finish();
        }
    }
}
