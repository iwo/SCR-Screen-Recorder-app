package com.iwobanas.screenrecorder;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class ShellRunner {
    private static final String TAG = "ShellRunner";

    RecorderProcess process;

    public ShellRunner() {
        process = new RecorderProcess();
        process.start();
    }

    public void start() {
        process.startRecording("/sdcard/uitest.mp4");
    }

    public void stop() {
        process.stopRecording();
    }

}

class RecorderProcess extends Thread {

    private static final String TAG = "RecorderProcess";

    private Process process ;

    private OutputStream stdin;

    private volatile RecorderProcessState state = RecorderProcessState.NEW;

    @Override
    public void run() {
        state = RecorderProcessState.STARTING;
        try {
            Log.d(TAG, "Starting native process");
            process = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "/data/local/tmp/screenrec"});
            Log.d(TAG, "Native process started");
        } catch (IOException e) {
            Log.e(TAG, "Error starting a new native process", e);
        }
        if (process == null) {
            return;
        }

        stdin = process.getOutputStream();

        state = RecorderProcessState.READY;

        try {
            Log.d(TAG, "Waiting for native process to exit");
            process.waitFor();
            Log.d(TAG, "Native process finished");
        } catch (InterruptedException e) {
            Log.e(TAG, "Native process interrupted", e);
        }

        if (state == RecorderProcessState.STOPPING) {
            state = RecorderProcessState.FINISHED;
        } else {
            state = RecorderProcessState.ERROR;
        }

        Log.d(TAG, "Return value: " + process.exitValue());
    }

    public void startRecording(String fileName) {
        if (state != RecorderProcessState.READY) {
            Log.e(TAG, "Can't start recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        state = RecorderProcessState.RECORDING;
        runCommand(fileName);
    }

    public void stopRecording() {
        if (state != RecorderProcessState.RECORDING) {
            Log.e(TAG, "Can't stop recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        state = RecorderProcessState.STOPPING;
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
}

enum RecorderProcessState {
    NEW,
    STARTING,
    READY,
    RECORDING,
    STOPPING,
    FINISHED,
    ERROR
}
