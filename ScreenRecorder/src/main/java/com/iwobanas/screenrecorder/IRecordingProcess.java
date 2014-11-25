package com.iwobanas.screenrecorder;

public interface IRecordingProcess {
    public void initialize();
    boolean isReady();
    RecordingProcessState getState();

    void start(String fileName, String rotation);
    void stop();
    void destroy();

    void addObserver(RecordingProcessObserver observer);
    void removeObserver(RecordingProcessObserver observer);

    interface RecordingProcessObserver {
        void onStateChange(IRecordingProcess process, RecordingProcessState state, RecordingInfo recordingInfo);
    }
}
