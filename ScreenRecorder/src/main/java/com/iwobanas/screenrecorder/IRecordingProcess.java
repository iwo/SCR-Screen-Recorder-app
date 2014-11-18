package com.iwobanas.screenrecorder;

public interface IRecordingProcess {
    public void initialize();
    boolean isReady();
    void start(String fileName, String rotation);
    void stop();
    void destroy();
}
