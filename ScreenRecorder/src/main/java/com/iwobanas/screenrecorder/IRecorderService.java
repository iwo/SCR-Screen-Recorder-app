package com.iwobanas.screenrecorder;

public interface IRecorderService {
    void startRecording();
    void stopRecording();
    void openLastFile();
    void close();

    void setReady(boolean ready);
    void recordingFinished();
    void suRequired();
    void startupError(int exitValue);
    void recordingError(int exitValue);

}
