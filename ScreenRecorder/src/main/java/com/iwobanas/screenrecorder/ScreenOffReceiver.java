package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.*;

public class ScreenOffReceiver extends android.content.BroadcastReceiver {

    private IRecorderService mService;

    private Context mContext;

    private boolean mIsRegistered;

    public ScreenOffReceiver(IRecorderService service, Context context) {
        mService = service;
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mService.stopRecording();
        EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_SCREEN, null);
    }


    public void register() {
        if (mIsRegistered) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(this, intentFilter);
        mIsRegistered = true;
    }

    public void unregister() {
        if (!mIsRegistered) {
            return;
        }
        mContext.unregisterReceiver(this);
        mIsRegistered = false;
    }

}
