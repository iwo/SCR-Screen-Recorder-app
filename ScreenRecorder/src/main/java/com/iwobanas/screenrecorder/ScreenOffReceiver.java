package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.*;

public class ScreenOffReceiver extends android.content.BroadcastReceiver {

    private IRecorderService service;

    private Context context;

    private boolean isRegistered;

    public ScreenOffReceiver(IRecorderService service, Context context) {
        this.service = service;
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        service.stopRecording();
        EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_SCREEN, null);
    }


    public void register() {
        if (isRegistered) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(this, intentFilter);
        isRegistered = true;
    }

    public void unregister() {
        if (!isRegistered) {
            return;
        }
        context.unregisterReceiver(this);
        isRegistered = false;
    }

}
