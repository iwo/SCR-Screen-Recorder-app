package com.iwobanas.screenrecorder.audio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.iwobanas.screenrecorder.RecorderService;
import com.iwobanas.screenrecorder.settings.Settings;

public class AudioWarningActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        AudioWarningDialogFragment dialogFragment = (AudioWarningDialogFragment) getFragmentManager().findFragmentByTag(AudioWarningDialogFragment.FRAGMENT_TAG);
        if (dialogFragment == null) {
            dialogFragment = new AudioWarningDialogFragment();
            dialogFragment.show(getFragmentManager(), AudioWarningDialogFragment.FRAGMENT_TAG);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioWarningDialogFragment dialogFragment = (AudioWarningDialogFragment) getFragmentManager().findFragmentByTag(AudioWarningDialogFragment.FRAGMENT_TAG);
        if (dialogFragment != null) {
            dialogFragment.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(RecorderService.LOUNCHER_ACTION);
        startService(intent);
    }
}
