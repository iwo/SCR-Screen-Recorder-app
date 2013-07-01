package com.iwobanas.screenrecorder;

public interface IRecorderService {
    void startRecording();
    void stopRecording();
    void close();

    void setReady(boolean ready);
    void recordingFinished();
    void suRequired();
    void startupError(int exitValue);
    void recordingError(int exitValue);
    void mediaRecorderError(int exitValue);

    boolean getMicAudio();
    void setMicAudio(boolean micAudio);

    void showSettings();
}
