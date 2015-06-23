package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

public class MediaProjectionActivity extends Activity {
    private static final int PERMISSION_CODE = 1;

    private final String PREFERENCES_NAME = "MediaProjectionActivity";
    private final String SYSTEM_UI_CRASH_MESSAGE = "system_ui_crash_message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestMediaProjection();
    }

    public void requestMediaProjection() {
        boolean permanent = Utils.isMediaProjectionPermanent(this);
        boolean affectedVersion = Build.VERSION.RELEASE.equals("5.1") || Build.VERSION.RELEASE.equals("5.1.0");
        if (affectedVersion && permanent && !getSharedPreferences(PREFERENCES_NAME, 0).getBoolean(SYSTEM_UI_CRASH_MESSAGE, false)) {
            getSharedPreferences(PREFERENCES_NAME, 0).edit().putBoolean(SYSTEM_UI_CRASH_MESSAGE, true).apply();
            new SystemUICrashDialogFragment().show(getFragmentManager(), SystemUICrashDialogFragment.FRAGMENT_TAG);
        } else {
            if (affectedVersion && !permanent) {
                Toast.makeText(this, getString(R.string.system_ui_crash_warning_toast, getString(R.string.media_projection_remember_text)), Toast.LENGTH_SHORT).show();
            }
            MediaProjectionManager mediaProjectionManager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                    PERMISSION_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_CODE) {
            Intent serviceIntent = new Intent(this, RecorderService.class);
            if (resultCode != RESULT_OK) {
                if (RecorderService.isRunning()) {
                    serviceIntent.setAction(RecorderService.PROJECTION_DENY_ACTION);
                    startService(serviceIntent);
                }
            } else {
                serviceIntent.setAction(RecorderService.SET_PROJECTION_ACTION);
                serviceIntent.putExtra(RecorderService.PROJECTION_DATA_EXTRA, data);
                startService(serviceIntent);
            }
        } else {
            //TODO: report error to analytics
        }
        finish();
    }
}
