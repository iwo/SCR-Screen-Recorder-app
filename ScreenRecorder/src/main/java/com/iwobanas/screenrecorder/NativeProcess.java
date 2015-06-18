package com.iwobanas.screenrecorder;

import android.content.Context;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicInteger;

import static com.iwobanas.screenrecorder.Tracker.*;

class NativeProcess implements Runnable, INativeCommandRunner {

    private static final String MEDIASERVER_COMMAND = "/system/bin/mediaserver";
    private static AtomicInteger threadNumber = new AtomicInteger(0);
    private final String TAG = "scr_RecorderProcess-" + threadNumber.get();

    private Process process;
    private OutputStreamWriter outputWriter;
    private BufferedReader inputReader;
    private volatile ProcessState state = ProcessState.NEW;
    private Context context;
    private String executable;
    private OnStateChangeListener onStateChangeListener;
    private RecordingInfo recordingInfo = new RecordingInfo();
    private boolean destroying = false;
    private String suVersion;
    private boolean execBlocked;

    public NativeProcess(Context context, OnStateChangeListener onStateChangeListener) {
        this.context = context;
        this.onStateChangeListener = onStateChangeListener;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("NativeProcess-" + threadNumber.getAndIncrement());
            setState(ProcessState.INITIALIZING);
            installExecutable();
            if (state != ProcessState.INITIALIZING) {
                return;
            }
            startProcess();

            while (state != ProcessState.DONE) {
                readUpdate();
            }
        } catch (NativeProcessException e) {
            Log.e(TAG, "Error executing native process", e);
            setErrorState(305);
        }
        waitForExit();
        setState(ProcessState.DEAD);
    }

    private void installExecutable() {
        File file = new File(context.getFilesDir(), "screenrec");
        try {
            executable = file.getAbsolutePath();
            if (Utils.isArm()) {
                Utils.extractResource(context, R.raw.screenrec, file);
            } else if (Utils.isX86()) {
                Utils.extractResource(context, R.raw.screenrec_x86, file);
            } else {
                setState(ProcessState.CPU_NOT_SUPPORTED_ERROR);
                return;
            }

            if (!file.setExecutable(true, false)) {
                Log.w(TAG, "Can't set executable property on " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "Can't install native executable", e);
            setState(ProcessState.INSTALLATION_ERROR);
            EasyTracker.getTracker().sendEvent(ERROR, INSTALLATION_ERROR, INSTALLATION_ERROR, null);
            EasyTracker.getTracker().sendException(Thread.currentThread().getName(), e, false);
        }
    }

    private void startProcess() throws NativeProcessException {
        try {
            Log.d(TAG, "Starting native process");
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", executable});
            Log.d(TAG, "Native process started");
        } catch (IOException e) {
            Log.e(TAG, "Error starting a new native process", e);
        }
        if (process == null) {
            throw new NativeProcessException("Process is null");
        }
        outputWriter = new OutputStreamWriter(process.getOutputStream());
        inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        new Thread(new ErrorStreamReader(process.getErrorStream())).start();
    }

    private void readUpdate() throws NativeProcessException {
        String line;
        try {
            line = inputReader.readLine();
            if (line == null) {
                throw new NativeProcessException("Error reading status update");
            }

            if (line.startsWith("state ")) {
                parseState(line);
            } else if (line.startsWith("rotateView ")) {
                parseInputParams(line);
            } else if (line.startsWith("fps ")) {
                parseFps(line);
            } else if (line.startsWith("error ")) {
                parseError(line);
            } else if (line.startsWith("su version ")) {
                parseSuVersion(line);
            } else if (line.startsWith("command result ")) {
                parseCommandResult(line);
            } else if (line.length() > 0) {
                Log.e(TAG, "Unexpected update: " + line);
            }
        } catch (IOException e) {
            throw new NativeProcessException("Exception while reading status", e);
        }
    }

    private void parseSuVersion(String line) {
        suVersion = line.substring("su version ".length());
        if ("exec_error".equals(suVersion)) {
            Log.v(TAG, "Exec blocked from native process");
            execBlocked = true;
            ShellCommand suCommand = new ShellCommand(new String[]{"su", "-v"});
            suCommand.setOutLogTag("su -v");
            suCommand.setInput("exit\n"); // just in case su doesn't support -v and interactive shell gets opened
            suCommand.setTimeoutMillis(3000);
            suCommand.execute();
            suVersion = suCommand.getOutput();
        }
        Log.v(TAG, "su version: " + suVersion);
    }

    private void waitForExit() {
        if (process != null) {
            try {
                process.waitFor();
                Log.v(TAG, "Process exit value: " + process.exitValue());
            } catch (InterruptedException e) {
                Log.w(TAG, "Process interrupted", e);
            }
        }
    }

    private void setErrorState(int exitValue) {
        ProcessState previousState = state;
        if (previousState != ProcessState.ERROR) {
            recordingInfo.exitValue = exitValue;
            setState(ProcessState.ERROR);
        } else {
            if (exitValue != recordingInfo.exitValue) {
                Log.w(TAG, "Exit value already set to " + recordingInfo.exitValue + " not updating to " + exitValue);
            }
            return;
        }

        if (previousState != ProcessState.NEW && previousState != ProcessState.INITIALIZING
                && mediaServerRelatedError()) {
            killMediaServer();
        }
    }

    private boolean mediaServerRelatedError() {
        if (destroying)
            return false;
        if (Settings.getInstance().getVideoEncoder() < 0)
            return false;
        switch (recordingInfo.exitValue) {
            case 201:
            case 216:
            case 217:
            case 219:
            case 221:
            case 223:
            case 227:
            case 226:
            case 229:
            case 237: // microphone busy
            case 251: // microphone busy
            case 305: // shell death
            case 306: // command write error
                return false;
            default:
                return true;
        }
    }

    private void parseFps(String fpsString) {
        try {
            recordingInfo.fps = Float.parseFloat(fpsString.substring(4));
        } catch (NumberFormatException e) {
            recordingInfo.fps = -1;
        }

        if (!destroying && recordingInfo.fps < 0) {
            Log.e(TAG, "Incorrect fps value received \"" + fpsString + "\"");
        }
    }

    private void parseCommandResult(String resultLine) {
        String[] tokens = resultLine.split("\\|");
        if (tokens.length < 4) {
            Log.e(TAG, "invalid command result format: " + resultLine);
        }
        int result;
        int requestId;
        try {
            requestId = Integer.valueOf(tokens[1]);
            result = Integer.valueOf(tokens[2]);
        } catch (NumberFormatException e) {
            requestId = -1;
            result = -1000;
        }
        NativeCommands.getInstance().notifyCommandResult(requestId, result);
    }

    private void parseError(String errorString) {
        int exitValue;
        try {
            exitValue = Integer.parseInt(errorString.substring("error ".length()));
        } catch (NumberFormatException e) {
            exitValue = 257;
        }

        setErrorState(exitValue);
    }

    private void parseState(String stateLine) {
        String stateString = stateLine.substring("state ".length());
        try {
            setState(ProcessState.valueOf(stateString));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Incorrect state", e);
        }
    }

    private void parseInputParams(String params) {
        Log.v(TAG, "Input params: " + params);
        boolean parsed = false;
        if (params != null && params.startsWith("rotateView")) {
            String[] kv = params.split("\\s");
            try {
                recordingInfo.rotateView = Integer.parseInt(kv[1]);
                recordingInfo.verticalInput = Integer.parseInt(kv[3]);
                recordingInfo.adjustedRotation = Integer.parseInt(kv[5]);
                parsed = true;
            } catch (NumberFormatException ignored) {
            }
        }
        if (!destroying && !parsed) {
            Log.e(TAG, "Incorrect input params received \"" + params + "\"");
        }
    }

    public ProcessState getState() {
        return state;
    }

    private void setState(ProcessState state) {
        Log.d(TAG, "setState " + state);
        ProcessState previousState = this.state;
        this.state = state;
        if (state == ProcessState.READY) {
            NativeCommands.getInstance().setCommandRunner(this);
        }
        if (!destroying && onStateChangeListener != null) {
            onStateChangeListener.onStateChange(this, state, previousState, recordingInfo);
        }
    }

    public void startRecording(File file, String rotation) {
        Log.i(TAG, "startRecording " + file.getAbsolutePath());
        if (state != ProcessState.READY) {
            Log.e(TAG, "Can't start recording in current state: " + state);
            //TODO: add error handling
            return;
        }
        recordingInfo = new RecordingInfo();
        recordingInfo.file = file;
        recordingInfo.rotation = rotation;
        Settings settings = Settings.getInstance();
        setState(ProcessState.STARTING);
        settings.updateAudioDriverConfig();
        String audioSource;
        if (settings.getTemporaryMute()) {
            Log.v(TAG, "Audio muted for this recording");
            audioSource = AudioSource.MUTE.getCommand();
            settings.setTemporaryMute(false);
        } else if (settings.getTimeLapse() != 1) {
            Log.v(TAG, "Audio muted for time-lapse recording");
            audioSource = AudioSource.MUTE.getCommand();
        } else {
            audioSource = settings.getAudioSource().getCommand();
        }

        int frameRate = settings.getFrameRate();
        if (settings.getTimeLapse() != 1) {
            // use 60fps if frame rate is set to Max
            if (frameRate == -1) {
                frameRate = 60;
            }
            frameRate /= settings.getTimeLapse();
        }

        String startCommand = "start " + rotation + " " + audioSource + " "
                + settings.getResolution().getVideoWidth() + " "
                + settings.getResolution().getVideoHeight() + " "
                + settings.getResolution().getPaddingWidth() + " "
                + settings.getResolution().getPaddingHeight() + " "
                + frameRate + " "
                + settings.getTransformation().name() + " "
                + (settings.getColorFix() ? "BGRA" : "RGBA") + " "
                + settings.getVideoBitrate().getCommand() + " "
                + settings.getSamplingRate().getSamplingRate() + " "
                + (settings.getStereo() ? 2 : 1) + " "
                + settings.getVideoEncoder() + " "
                + (settings.getVerticalFrames() ? 1 : 0) + " "
                + fixEmulatedStorageMapping(file.getAbsolutePath());
        runCommand(startCommand);
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
                    " timeLapse: " + settings.getTimeLapse() +
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
    }

    @Override
    public String getSuVersion() {
        return suVersion;
    }

    @Override
    public boolean isExecBlocked() {
        return execBlocked;
    }

    @Override
    public String getExecutable() {
        return executable;
    }

    @Override
    public boolean runCommand(String command, int requestId, String args) {
        Log.v(TAG, "Run command: " + command);
        String commandLine = command + " " + requestId + " " + args + "\n";
        try {
            outputWriter.write(commandLine);
            outputWriter.flush();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error running command", e);
            return false;
        }
    }

    private void runCommand(String command) {
        try {
            outputWriter.write(command + "\n");
            outputWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error running command", e);
            setErrorState(306);
        }
    }

    public void destroy() {
        if (process != null) {
            if (state == ProcessState.RECORDING) {
                stopRecording();
            }
            Log.d(TAG, "Destroying process");
            destroying = true;
            if (state != ProcessState.DEAD) {
                runCommand("quit");
            }
        }
    }

    private void killMediaServer() {
        Log.d(TAG, "restartMediaServer");
        CameraOverlay.releaseCamera();
        killProcess(MEDIASERVER_COMMAND);
        if (Utils.waitForProcess(MEDIASERVER_COMMAND, 7000) != -1) {
            CameraOverlay.reconnectCamera();
        }
    }

    private void killProcess(String command) {
        Log.d(TAG, "kill process " + command);
        int pid = Utils.findProcessByCommand(command);
        if (pid == -1 || pid == 0) {
            Log.e(TAG, command + " process not found");
            return;
        }
        NativeCommands.getInstance().killSignal(pid);
    }

    private void forceStop() {
        runCommand("force_stop");
    }

    public void startTimeout() {
        forceStop();
        setErrorState(302);
    }

    public void stopTimeout() {
        forceStop();
        setErrorState(303);
    }

    public static interface OnStateChangeListener {
        void onStateChange(NativeProcess target, ProcessState state, ProcessState previousState, RecordingInfo recordingInfo);
    }

    public static enum ProcessState {
        NEW,
        INITIALIZING,
        READY,
        STARTING,
        RECORDING,
        STOPPING,
        FINISHED,
        CPU_NOT_SUPPORTED_ERROR,
        INSTALLATION_ERROR,
        ERROR,
        DONE,
        DEAD
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
            } catch (IOException ignored) {
            }
        }
    }

    class NativeProcessException extends Exception {
        public NativeProcessException(String msg) {
            super(msg);
        }

        NativeProcessException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
