package com.iwobanas.screenrecorder;

import java.io.File;

public interface IRecordingProcess {
    public void initialize();
    boolean isReady();
    RecordingProcessState getState();

    void start(File file, String rotation);
    void stop();
    void startTimeout();
    void stopTimeout();
    void destroy();

    void addObserver(RecordingProcessObserver observer);
    void removeObserver(RecordingProcessObserver observer);

    interface RecordingProcessObserver {
        void onStateChange(IRecordingProcess process, RecordingProcessState state, RecordingInfo recordingInfo);
    }
}
