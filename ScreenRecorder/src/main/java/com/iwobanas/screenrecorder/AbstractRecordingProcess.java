package com.iwobanas.screenrecorder;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractRecordingProcess implements IRecordingProcess {

    private final String logTag;
    private Handler handler;
    private Collection<RecordingProcessObserver> observers = new ArrayList<>(5);
    private volatile RecordingProcessState state;

    private long startTimeoutMillis;
    private long stopTimeoutMillis;
    private Handler timeoutsHandler;
    private HandlerThread timeoutsThread = new HandlerThread("Timeouts thread");

    private final Runnable startTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            startTimeout();
        }
    };

    private final Runnable stopTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            stopTimeout();
        }
    };

    protected AbstractRecordingProcess(String logTag, long startTimeoutMillis, long stopTimeoutMillis) {
        this.logTag = logTag;
        this.startTimeoutMillis = startTimeoutMillis;
        this.stopTimeoutMillis = stopTimeoutMillis;
        timeoutsThread.start();
        timeoutsHandler = new Handler(timeoutsThread.getLooper());
        handler = new Handler();
    }

    protected void notifyObservers(final RecordingProcessState state, final RecordingInfo recordingInfo) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (RecordingProcessObserver observer : observers) {
                    observer.onStateChange(AbstractRecordingProcess.this, state, recordingInfo);
                }
            }
        });
    }

    @Override
    public void removeObserver(RecordingProcessObserver observer) {
        if (observers.contains(observer)) {
            observers.remove(observer);
        }
    }

    @Override
    public void addObserver(RecordingProcessObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public boolean isReady() {
        return state == RecordingProcessState.READY;
    }

    @Override
    public RecordingProcessState getState() {
        return state;
    }

    protected void setState(RecordingProcessState state, RecordingInfo recordingInfo) {
        Log.v(logTag, state.name());
        if (this.state == state) {
            return;
        }
        this.state = state;
        updateTimeouts();
        notifyObservers(state, recordingInfo);
    }

    private void updateTimeouts() {
        if (state == RecordingProcessState.STARTING) {
            timeoutsHandler.postDelayed(startTimeoutRunnable, startTimeoutMillis);
        } else {
            timeoutsHandler.removeCallbacks(startTimeoutRunnable);
        }

        if (state == RecordingProcessState.STOPPING) {
            timeoutsHandler.postDelayed(stopTimeoutRunnable, stopTimeoutMillis);
        } else {
            timeoutsHandler.removeCallbacks(stopTimeoutRunnable);
        }
    }
}
