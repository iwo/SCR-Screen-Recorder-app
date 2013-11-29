package com.iwobanas.screenrecorder;

import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import static com.iwobanas.screenrecorder.Tracker.*;

class RecorderProcess implements Runnable {

    private static int instancesCount = 0;

    private final String TAG = "scr_RecorderProcess-" + instancesCount++;

    private Process process ;

    private OutputStream stdin;

    private InputStream stdout;

    private volatile ProcessState state = ProcessState.NEW;

    private String executable;

    private OnStateChangeListener onStateChangeListener;

    private int exitValue = -1;

    private Integer exitValueOverride;

    private float fps = -1.0f;

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

            new Thread(new ErrorStreamReader(process.getErrorStream())).start();

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

                    status = reader.readLine();
                    parseFps(status);
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception when reading state", e);
                exitValueOverride = 307;
                forceKill();
            }

            try {
                Log.d(TAG, "Flushing output");
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.e(TAG, "unexpected output: " + line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error when flushing stdout", e);
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

        if (destroying && (exitValue == 200 || exitValue == 222)) {
            setState(ProcessState.FINISHED);
        } else if (exitValueOverride != null) {
            if (exitValue < 200) {
                exitValue = exitValueOverride;
            }
            setErrorState();
        } else if (state == ProcessState.STOPPING) {
            setState(ProcessState.FINISHED);
        } else {
            setErrorState();
        }

        Log.d(TAG, "Return value: " + exitValue);
    }

    private void setErrorState() {
        setState(ProcessState.ERROR);
        if (mediaServerRelatedError()) {
            killMediaServer();
        }
    }

    private boolean mediaServerRelatedError() {
        if (destroying)
            return false;
        if (Settings.getInstance().getVideoEncoder() < 0)
            return false;
        switch (exitValue) {
            case 216:
            case 217:
            case 219:
            case 221:
            case 223:
            case 227:
            case 226:
            case 229:
                return false;
            default:
                return true;
        }
    }


    private void checkStatus(String expectedStatus, String status, int errorCode) {
        if (forceKilled || destroying || status == null) return;
        if (!expectedStatus.equals(status)) {
            Log.e(TAG, "Incorrect status received: " + status);
            exitValueOverride = errorCode;
            forceKill();
        }
    }

    private void parseFps(String fpsString) {
        if (fpsString != null && fpsString.startsWith("fps ") && fpsString.length() > 4) {
            try {
                fps = Float.parseFloat(fpsString.substring(4));
            } catch (NumberFormatException e) {
                fps = -1;
            }
        }
        if (!destroying && fps < 0) {
            Log.e(TAG, "Incorrect fps value received \"" + fpsString + "\"");
        }
    }

    private void setState(ProcessState state) {
        Log.d(TAG, "setState " + state);
        ProcessState previousState = this.state;
        this.state = state;
        if (!destroying && onStateChangeListener != null) {
            onStateChangeListener.onStateChange(this, state, previousState, exitValue, fps);
        }
    }

    public void startRecording(String fileName, String rotation) {
        Log.i(TAG, "startRecording " + fileName);
        if (state != ProcessState.READY) {
            Log.e(TAG, "Can't start recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        Settings settings = Settings.getInstance();
        setState(ProcessState.RECORDING);
        configureTimeout.start();
        startTimeout.start();
        runCommand(fixEmulatedStorageMapping(fileName));
        runCommand(rotation);
        runCommand(settings.getAudioSource().getCommand());
        runCommand(String.valueOf(settings.getResolution().getVideoWidth()));
        runCommand(String.valueOf(settings.getResolution().getVideoHeight()));
        runCommand(String.valueOf(settings.getResolution().getPaddingWidth()));
        runCommand(String.valueOf(settings.getResolution().getPaddingHeight()));
        runCommand(String.valueOf(settings.getFrameRate()));
        runCommand(settings.getTransformation().name());
        runCommand(settings.getColorFix() ? "BGRA" : "RGBA");
        runCommand(settings.getVideoBitrate().getCommand());
        if (settings.getAudioSource().equals(AudioSource.INTERNAL)) {
            runCommand(String.valueOf(settings.getAudioDriver().getSamplingRate()));
        } else {
            runCommand(settings.getSamplingRate().getCommand());
        }
        runCommand(String.valueOf(settings.getVideoEncoder()));
        runCommand(String.valueOf(settings.getVerticalFrames() ? 1 : 0));
        logSettings(settings, rotation);
    }

    private String fixEmulatedStorageMapping(String fileName) {
        String emulatedSrc = System.getenv("EMULATED_STORAGE_SOURCE");
        String emulatedTarget = System.getenv("EMULATED_STORAGE_TARGET");

        if (emulatedSrc == null || emulatedTarget == null || !fileName.startsWith(emulatedTarget)) {
            return fileName;
        }
        return fileName.replaceFirst(emulatedTarget, emulatedSrc);
    }

    private void logSettings(Settings settings, String rotation) {
        try {
            Log.d(TAG, "settings rotation: " + rotation +
                    " audioSource: " + settings.getAudioSource().name() +
                    " resolution: " + settings.getResolution().getWidth() + " x " + settings.getResolution().getHeight() +
                    " frameRate: " + settings.getFrameRate() +
                    " transformation: " + settings.getTransformation().name() +
                    " videoBitrate: " + settings.getVideoBitrate().name() +
                    " samplingRate: " + settings.getSamplingRate().name() +
                    " colorFix: " + settings.getColorFix() +
                    " videoEncoder: " + settings.getVideoEncoder() +
                    " verticalFrames: " + settings.getVerticalFrames()
            );
        } catch (Throwable e) {
            Log.w(TAG, "Can't log settings");
        }
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
            stopProcess();
        }
    }

    private void stopProcess() {
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

        killProcess(executable);
    }

    private void killMediaServer() {
        Log.d(TAG, "restartMediaServer");
        killProcess("/system/bin/mediaserver");
    }

    private void killProcess(String command) {
        Log.d(TAG, "kill process " + command);
        int pid = Utils.findProcessByCommand(command);
        if (pid == -1 || pid == 0) {
            Log.e(TAG, command + " process not found");
            return;
        }
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "kill -9 " + pid});
        } catch (IOException e) {
            Log.e(TAG, "error killing " + command, e);
        }
    }

    public static interface OnStateChangeListener {
        void onStateChange(RecorderProcess target, ProcessState state, ProcessState previousState, int exitValue, float fps);
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

    class ErrorStreamReader implements Runnable {
        private BufferedReader reader;

        public ErrorStreamReader(InputStream stream) {
            reader = new BufferedReader(new InputStreamReader(stream));
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    Log.w(TAG, "stderr: " + line);
                }
            } catch (IOException ignored) {}
        }
    }
}
