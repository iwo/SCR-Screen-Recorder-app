package com.iwobanas.screenrecorder;

public interface IRecorderService {
    void startRecording();
    void stopRecording();
    void close();

    void setReady();
    void executableInstalled(String executable);
    void recordingStarted();
    void recordingFinished(RecordingInfo recordingInfo);
    void suRequired();
    void cpuNotSupportedError();
    void installationError();
    void startupError(RecordingInfo recordingInfo);
    void videoCodecError(RecordingInfo recordingInfo);
    void recordingError(RecordingInfo recordingInfo);
    void mediaRecorderError(RecordingInfo recordingInfo);
    void maxFileSizeReached(RecordingInfo recordingInfo);
    void outputFileError(RecordingInfo recordingInfo);
    void microphoneBusyError(RecordingInfo recordingInfo);
    void openGlError(RecordingInfo recordingInfo);
    void secureSurfaceError(RecordingInfo recordingInfo);
    void audioConfigError(RecordingInfo recordingInfo);

    void showSettings();
    void showTimeoutDialog();

    String getDeviceId();
}
