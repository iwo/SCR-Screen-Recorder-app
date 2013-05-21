package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ScreenOffReceiver extends android.content.BroadcastReceiver {

    private IRecorderService mService;

    public ScreenOffReceiver(IRecorderService service) {
        mService = service;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mService.stopRecording();
    }


    public void register(Context context) {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(this, intentFilter);
    }

    public void unregister(Context context) {
        context.unregisterReceiver(this);
    }

}
