package com.iwobanas.screenrecorder;

public interface IRecorderService {
    void startRecording();
    void stopRecording();
    void close();

    void executableInstalled(String executable);

    void showSettings();
    void showTimeoutDialog();
}
