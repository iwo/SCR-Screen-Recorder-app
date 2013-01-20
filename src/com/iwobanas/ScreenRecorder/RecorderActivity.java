package com.iwobanas.ScreenRecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class RecorderActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, RecorderService.class);
        startService(intent);
        finish();
    }
}
