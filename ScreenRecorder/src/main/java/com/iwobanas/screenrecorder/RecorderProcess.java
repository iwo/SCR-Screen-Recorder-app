package com.iwobanas.screenrecorder;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

class RecorderProcess extends Thread {

    private static final String TAG = "RecorderProcess";

    private Process process ;

    private OutputStream stdin;

    private volatile ProcessState state = ProcessState.NEW;

    private String executable;

    private OnStateChangeListener onStateChangeListener;

    private int exitValue;

    public RecorderProcess(String executable, OnStateChangeListener onStateChangeListener) {
        this.executable = executable;
        this.onStateChangeListener = onStateChangeListener;
    }

    @Override
    public void run() {
        setState(ProcessState.STARTING);
        try {
            Log.d(TAG, "Starting native process");
            process = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", executable});
            Log.d(TAG, "Native process started");
        } catch (IOException e) {
            Log.e(TAG, "Error starting a new native process", e);
        }
        if (process != null) {
            stdin = process.getOutputStream();

            setState(ProcessState.READY);

            try {
                Log.d(TAG, "Waiting for native process to exit");
                process.waitFor();
                Log.d(TAG, "Native process finished");
            } catch (InterruptedException e) {
                Log.e(TAG, "Native process interrupted", e);
            }

            exitValue = process.exitValue();
        }

        if (state == ProcessState.STOPPING) {
            setState(ProcessState.FINISHED);
        } else {
            setState(ProcessState.ERROR);
        }

        Log.d(TAG, "Return value: " + exitValue);
    }

    private void setState(ProcessState state) {
        ProcessState previousState = this.state;
        this.state = state;
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChange(state, previousState, exitValue);
        }
    }

    public void startRecording(String fileName, String rotation, boolean micAudio) {
        if (state != ProcessState.READY) {
            Log.e(TAG, "Can't start recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        setState(ProcessState.RECORDING);
        runCommand(fileName);
        runCommand(rotation);
        runCommand(micAudio ? "m" : "x");
    }

    public void stopRecording() {
        if (state != ProcessState.RECORDING) {
            Log.e(TAG, "Can't stop recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        setState(ProcessState.STOPPING);
        runCommand("stop");
    }

    private void runCommand(String command) {
        try {
            command = command + "\n";
            stdin.write(command.getBytes());
            stdin.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error running command", e);
        }
    }

    public boolean isStopped() {
        return state == ProcessState.FINISHED || state == ProcessState.ERROR;
    }

    public static interface OnStateChangeListener {
        void onStateChange(ProcessState state, ProcessState previousState, int exitValue);
    }

    public static enum ProcessState {
        NEW,
        STARTING,
        READY,
        RECORDING,
        STOPPING,
        FINISHED,
        ERROR
    }
}
