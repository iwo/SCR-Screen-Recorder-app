package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.iwobanas.screenrecorder.settings.Settings;

public class NoRootModeActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        Settings.getInstance().setRootEnabled(false);
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(RecorderService.LOUNCHER_ACTION);
        startService(intent);
        finish();
    }
}
