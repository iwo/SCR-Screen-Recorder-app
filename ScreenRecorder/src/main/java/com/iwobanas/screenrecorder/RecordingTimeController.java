package com.iwobanas.screenrecorder;

import android.os.Handler;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.*;

public class RecordingTimeController {
    private final int TIMEOUT = 3 * 60 * 1000;
    private final int DIALOG_TIMEOUT = TIMEOUT - 2000;


    private IRecorderService service;
    private Handler handler;

    private final Runnable showDialogRunnable = new Runnable() {
        @Override
        public void run() {
            service.showTimeoutDialog();
        }
    };

    private final Runnable stopRecordingRunnable = new Runnable() {
        @Override
        public void run() {
            service.stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_TIME, null);
        }
    };

    public RecordingTimeController(IRecorderService service) {
        this.service = service;
        handler = new Handler();
    }

    public void start() {
        handler.postDelayed(showDialogRunnable, DIALOG_TIMEOUT);
        handler.postDelayed(stopRecordingRunnable, TIMEOUT);
    }

    public void reset() {
        handler.removeCallbacks(showDialogRunnable);
        handler.removeCallbacks(stopRecordingRunnable);
    }
}
