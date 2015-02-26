package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

public class MediaProjectionActivity extends Activity {
    private static final int PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                PERMISSION_CODE);
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
