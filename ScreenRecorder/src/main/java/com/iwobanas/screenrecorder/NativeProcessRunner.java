package com.iwobanas.screenrecorder;

import android.util.Log;

public class NativeProcessRunner implements RecorderProcess.OnStateChangeListener {
    private static final String TAG = "NativeProcessRunner";

    IRecorderService service;

    RecorderProcess process;

    private String executable;

    public NativeProcessRunner(IRecorderService service) {
        this.service = service;
    }

    public void start(String fileName, String rotation) {
        process.startRecording(fileName, rotation);
    }

    public void stop() {
        process.stopRecording();
    }

    public void initialize() {
        if (process == null || process.isStopped()) {
            process = new RecorderProcess(executable, this);
            process.start();
        }
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    @Override
    public void onStateChange(RecorderProcess.ProcessState state, RecorderProcess.ProcessState previousState, int exitValue) {
        switch (state) {
            case READY:
                service.setReady(true);
                break;
            case FINISHED:
                service.recordingFinished();
                service.setReady(false);
                break;
            case ERROR:
                if (previousState == RecorderProcess.ProcessState.RECORDING
                    || previousState == RecorderProcess.ProcessState.FINISHED) {
                    handleRecordingError(exitValue);
                } else {
                    handleStartupError(exitValue);
                }
                service.setReady(false);
                break;
            default:
                break;
        }
    }

    private void handleStartupError(int exitValue) {
        if (exitValue == 1) { // general error e.g. SuperSu Deny access
            Log.e(TAG, "Error code 1. Assuming no super user access");
            service.suRequired();
        } else if (exitValue == 127) { // command not found
            //TODO: verify installation
            Log.e(TAG, "Error code 127. This may be an installation issue");
            service.startupError(exitValue);
        } else {
            logError(exitValue);
            service.startupError(exitValue);
        }
    }

    private void handleRecordingError(int exitValue) {
        logError(exitValue);
        service.recordingError(exitValue);
    }

    private void logError(int exitValue) {
        if (exitValue > 128 && exitValue < 165) { // UNIX signal
            Log.e(TAG, "UNIX signal received: " + (exitValue - 128));
        } else if (exitValue >= 200 && exitValue < 256) { // Application error
            Log.e(TAG, "Native application error: " + exitValue);
        } else {
            Log.e(TAG, "Unknown exit value: " + exitValue);
        }
    }
}
