package com.iwobanas.screenrecorder;

import android.content.Context;
import android.util.Log;

public class PresentationThreadRunner {
    private static final String TAG = "scr_PresentationProcessRunner";

    IRecorderService service;

    Context context;

    public PresentationThreadRunner(Context context, IRecorderService service) {
        this.context = context;
        this.service = service;
    }

    public void start(String fileName) {
        Log.i(TAG, "start deviceId: " + service.getDeviceId());
        service.recordingStarted();
    }

    public void stop() {
        RecordingInfo info = new RecordingInfo();
        info.exitValue = -1;
        service.recordingFinished(info);
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
    }
}
