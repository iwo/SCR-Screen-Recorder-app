package com.iwobanas.screenrecorder;

import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import static com.iwobanas.screenrecorder.Tracker.*;

class RecorderProcess implements Runnable{

    private static int instancesCount = 0;

    private final String TAG = "RecorderProcess-" + instancesCount++;

    private Process process ;

    private OutputStream stdin;

    private InputStream stdout;

    private volatile ProcessState state = ProcessState.NEW;

    private String executable;

    private OnStateChangeListener onStateChangeListener;

    private int exitValue = -1;

    private Integer exitValueOverride;

    private boolean destroying = false;

    private volatile boolean forceKilled = false;

    private Timeout configureTimeout = new Timeout(3000, RECORDING_ERROR, CONFIGURE_TIMEOUT, 301);

    private Timeout startTimeout = new Timeout(10000, RECORDING_ERROR, START_TIMEOUT, 302);

    private Timeout stopTimeout = new Timeout(10000, STOPPING_ERROR, STOP_TIMEOUT, 303);

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
            stdout = process.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            try {
                String status = reader.readLine();
                if (status != null) {
                    checkStatus("ready", status, 304);

                    setState(ProcessState.READY);

                    status = reader.readLine();
                    checkStatus("configured", status, 305);
                    configureTimeout.cancel();

                    status = reader.readLine();
                    checkStatus("recording", status, 306);
                    startTimeout.cancel();
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception when reading state", e);
                exitValueOverride = 307;
                forceKill();
            }

            try {
                Log.d(TAG, "Waiting for native process to exit");
                process.waitFor();
                Log.d(TAG, "Native process finished");
            } catch (InterruptedException e) {
                Log.e(TAG, "Native process interrupted", e);
            }

            stopTimeout.cancel();

            exitValue = process.exitValue();
        }

        if (exitValueOverride != null) {
            exitValue = exitValueOverride;
            setState(ProcessState.ERROR);
            if (!destroying) {
                killMediaServer();
            }
        } else if (state == ProcessState.STOPPING) {
            setState(ProcessState.FINISHED);
        } else {
            setState(ProcessState.ERROR);
            if (!destroying) {
                killMediaServer();
            }
        }

        Log.d(TAG, "Return value: " + exitValue);
    }

    private void checkStatus(String expectedStatus, String status, int errorCode) {
        if (forceKilled || destroying) return;
        if (!expectedStatus.equals(status)) {
            Log.e(TAG, "Incorrect status received: " + status);
            exitValueOverride = errorCode;
            forceKill();
        }
    }

    private void setState(ProcessState state) {
        Log.d(TAG, "setState " + state);
        ProcessState previousState = this.state;
        this.state = state;
        if (!destroying && onStateChangeListener != null) {
            onStateChangeListener.onStateChange(this, state, previousState, exitValue);
        }
    }

    public void startRecording(String fileName, String rotation) {
        Log.d(TAG, "startRecording");
        if (state != ProcessState.READY) {
            Log.e(TAG, "Can't start recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        Settings settings = Settings.getInstance();
        setState(ProcessState.RECORDING);
        configureTimeout.start();
        startTimeout.start();
        runCommand(fileName);
        runCommand(rotation);
        runCommand(settings.getAudioSource().getCommand());
        runCommand(String.valueOf(settings.getResolution().getWidth()));
        runCommand(String.valueOf(settings.getResolution().getHeight()));
        runCommand(String.valueOf(settings.getFrameRate()));
        runCommand(settings.getTransformation().name());
        runCommand(settings.getColorFix() ? "BGRA" : "RGBA");
    }

    public void stopRecording() {
        Log.d(TAG, "stopRecording");
        if (state != ProcessState.RECORDING) {
            Log.e(TAG, "Can't stop recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        setState(ProcessState.STOPPING);
        runCommand("stop");
        stopTimeout.start();
    }

    class Timeout {
        private int time;
        private String errorName;
        private String errorCategory;
        private int errorCode;
        private Timer timer;

        public Timeout(int time, String errorCategory, String errorName, int errorCode) {
            this.time = time;
            this.errorCategory = errorCategory;
            this.errorName = errorName;
            this.errorCode = errorCode;
        }

        public void start() {
            synchronized (RecorderProcess.this) {
                if (timer != null) {
                    Log.e(TAG, "Timeout already started");
                    return;
                }
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (process != null) {
                            Log.w(TAG, "Timeout, killing the native process" + errorName);
                            EasyTracker.getTracker().sendEvent(ERROR, errorCategory, errorName, null);
                            exitValueOverride = errorCode;
                            forceKill();
                            timer = null;
                        }
                    }
                }, time);
            }
        }

        public void cancel() {
            synchronized (RecorderProcess.this) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        }
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

    public boolean isRecording() {
        return state == ProcessState.RECORDING;
    }

    public void destroy() {
        if (process != null) {
            Log.d(TAG, "Destroying process");
            destroying = true;
            stopTimeout.start();
            killProcess();
        }
    }

    private void killProcess() {
        if (process != null) try {
            // process.destroy(); fails with "EPERM (Operation not permitted)"
            // so we just close the input stream
            stdin.close();
        } catch (IOException ignored) {
        }
    }

    private synchronized void forceKill() {
        Log.d(TAG, "forceKill");
        if (forceKilled) {
            Log.d(TAG, "Already force killed");
            return;
        }
        forceKilled = true;

        try {
            Field f = process.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            Integer pid = (Integer) f.get(process);
            Log.d(TAG, "killing pid " + pid);
            Runtime.getRuntime().exec(new String[]{"su", "-c", "kill -9 "+ pid});
        } catch (Exception e){
            Log.e(TAG, "Error killing the process", e);
        }
    }

    private void killMediaServer() {
        Log.d(TAG, "restartMediaServer");
        int pid = Utils.findProcessByCommand("/system/bin/mediaserver");
        if (pid == -1) {
            Log.e(TAG, "mediaserver process not found");
        }
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "kill -9 " + pid});
        } catch (IOException e) {
            Log.e(TAG, "error killing mediaserver", e);
        }
    }

    public static interface OnStateChangeListener {
        void onStateChange(RecorderProcess target, ProcessState state, ProcessState previousState, int exitValue);
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
