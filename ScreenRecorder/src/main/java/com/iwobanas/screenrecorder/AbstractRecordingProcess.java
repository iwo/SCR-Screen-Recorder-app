package com.iwobanas.screenrecorder;

import android.os.Handler;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractRecordingProcess implements IRecordingProcess {

    private Handler handler;
    private Collection<RecordingProcessObserver> observers = new ArrayList<RecordingProcessObserver>(5);
    private volatile RecordingProcessState state;

    protected AbstractRecordingProcess() {
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
        this.state = state;
        notifyObservers(state, recordingInfo);
    }
}
