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
    void maxFileSizeReached(int exitValue);
    void outputFileError(int exitValue);
    void microphoneBusyError(int exitValue);
    void openGlError(int exitValue);
    void secureSurfaceError(int exitValue);

    void showSettings();
    void showTimeoutDialog();

    String getDeviceId();
}
