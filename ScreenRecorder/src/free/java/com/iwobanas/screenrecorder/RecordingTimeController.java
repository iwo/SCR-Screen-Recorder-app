package com.iwobanas.screenrecorder;

import android.os.Handler;

import java.util.Timer;
import java.util.TimerTask;

public class RecordingTimeController {

    private IRecorderService service;
    private Handler handler;
    private Timer timer;

    public RecordingTimeController(IRecorderService service) {
        this.service = service;
        handler = new Handler();
    }

    public void start() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                service.stopRecording();
            }
        }, 3*60*1000);
    }

    public void reset() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                timer.cancel();
            }
        });
    }
}
