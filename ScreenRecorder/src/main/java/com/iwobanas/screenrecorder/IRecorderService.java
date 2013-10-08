package com.iwobanas.screenrecorder;

public interface IRecorderService {
    void startRecording();
    void stopRecording();
    void close();

    void setReady(boolean ready);
    void recordingFinished(float fps);
    void suRequired();
    void startupError(int exitValue);
    void recordingError(int exitValue);
    void mediaRecorderError(int exitValue);
    void maxFileSizeReached();
    void outputFileError(int exitValue);

    void showSettings();
    void showTimeoutDialog();

    String getDeviceId();
}
