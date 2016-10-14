package com.iwobanas.screenrecorder;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.File;

import static com.iwobanas.screenrecorder.Tracker.*;

public class NativeProcessRunner extends AbstractRecordingProcess implements NativeProcess.OnStateChangeListener {
    private static final String TAG = "scr_NativeProcessRunner";

    private Context context;
    private NativeProcess process;
    private volatile boolean destroyed;

    public NativeProcessRunner(Context context) {
        super(TAG, 10000, 10000);
        this.context = context;
    }

    public void start(File file, String rotation) {
        process.startRecording(file, rotation);
    }

    public void stop() {
        process.stopRecording();
    }

    @Override
    public void startTimeout() {
        if (process == null) {
            Log.w(TAG, "Timeout for non-existent process");
        }
        Log.w(TAG, "Start timeout");
        process.startTimeout();
    }

    @Override
    public void stopTimeout() {
        if (process == null) {
            Log.w(TAG, "Timeout for non-existent process");
        }
        Log.w(TAG, "Stop timeout");
        process.stopTimeout();
    }

    public void initialize() {

        if (process == null || process.getState() == NativeProcess.ProcessState.DEAD) {
            setState(RecordingProcessState.INITIALIZING, null);
            process = new NativeProcess(context, this);
            new Thread(process).start();
        } else if (process.getState() != NativeProcess.ProcessState.FINISHED
                && process.getState() != NativeProcess.ProcessState.INITIALIZING
                && process.getState() != NativeProcess.ProcessState.READY) {
            try {
                throw new IllegalStateException();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Can't initialize process in state: " + process.getState(), e);
            }
        }
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        destroyed = true;
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    public void onStateChange(NativeProcess target, NativeProcess.ProcessState state, NativeProcess.ProcessState previousState, RecordingInfo recordingInfo) {
        if (target != process) {
            Log.w(TAG, "received state update from old process");
            return;
        }

        switch (state) {
            case NEW:
            case INITIALIZING:
                break;
            case READY:
                setState(RecordingProcessState.READY, recordingInfo);
                break;
            case STARTING:
                setState(RecordingProcessState.STARTING, recordingInfo);
                break;
            case RECORDING:
                setState(RecordingProcessState.RECORDING, recordingInfo);
                break;
            case STOPPING:
                setState(RecordingProcessState.STOPPING, recordingInfo);
                break;
            case FINISHED:
                if (processTimeLapse(recordingInfo)) {
                    setState(RecordingProcessState.FINISHED, recordingInfo);
                } else {
                    setState(RecordingProcessState.UNKNOWN_RECORDING_ERROR, recordingInfo);
                }
                break;
            case CPU_NOT_SUPPORTED_ERROR:
                setState(RecordingProcessState.CPU_NOT_SUPPORTED_ERROR, null);
                break;
            case INSTALLATION_ERROR:
                setState(RecordingProcessState.INSTALLATION_ERROR, null);
                break;
            case ERROR:
                if (previousState == NativeProcess.ProcessState.INITIALIZING) {
                    handleStartupError(recordingInfo);
                } else if (previousState == NativeProcess.ProcessState.RECORDING
                    || previousState == NativeProcess.ProcessState.STARTING
                    || previousState == NativeProcess.ProcessState.STOPPING
                    || previousState == NativeProcess.ProcessState.FINISHED) {
                    handleRecordingError(recordingInfo);
                } else {
                    logError(recordingInfo.exitValue);
                    setState(RecordingProcessState.UNKNOWN_STARTUP_ERROR, recordingInfo);
                }
                break;
            case DONE:
            case DEAD:
                if (!destroyed && !getState().isCritical()) {
                    process = null;
                    initialize();
                }
                break;
            default:
                break;
        }
    }

    private boolean processTimeLapse(RecordingInfo recordingInfo) {
        int timeLapse = Settings.getInstance().getTimeLapse();
        if (timeLapse == 1) {
            Log.w(TAG, "Time lapse not supported in open source version of SCR");
        }
        return true;
    }

    private void handleStartupError(RecordingInfo recordingInfo) {
        if (recordingInfo.exitValue == -1 || recordingInfo.exitValue == 1 || recordingInfo.exitValue == 255
                || recordingInfo.exitValue == 305 || recordingInfo.exitValue == 306) { // general error e.g. SuperSu Deny access
            Log.e(TAG, "Assuming no super user access. Error code " + recordingInfo.exitValue);
            setState(RecordingProcessState.SU_ERROR, recordingInfo);
            EasyTracker.getTracker().sendEvent(ERROR, SU_ERROR, recordingInfo.exitValue == -1 ? NO_SU : SU_DENY, null);
        } else if (recordingInfo.exitValue == 127) { // command not found
            Log.e(TAG, "Error code 127. This may be an installation issue");
            setState(RecordingProcessState.UNKNOWN_STARTUP_ERROR, recordingInfo);
        } else {
            logError(recordingInfo.exitValue);
            setState(RecordingProcessState.UNKNOWN_STARTUP_ERROR, recordingInfo);
        }
    }

    private void handleRecordingError(RecordingInfo recordingInfo) {
        logError(recordingInfo.exitValue);
        switch (recordingInfo.exitValue) {
            case 302: // start timeout
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setState(RecordingProcessState.SELINUX_ERROR, recordingInfo);
                    break;
                }
                // else fallback to mediaRecorderError()
            case 213: // start() error
            case 227: // MEDIA_RECORDER_EVENT_ERROR
            case 228: // MEDIA_RECORDER_TRACK_EVENT_ERROR - video
            case 197: // MEDIA_RECORDER_TRACK_EVENT_ERROR - audio
            case 248: // MEDIA_RECORDER_EVENT_ERROR during startup
            case 249: // MEDIA_RECORDER_TRACK_EVENT_ERROR during startup
            case 242: // dequeueBuffer
            case 243: // eglSwapBuffers
            case 245: // queueBuffer
            case 198: // SurfaceMediaSource error
                setState(RecordingProcessState.MEDIA_RECORDER_ERROR, recordingInfo);
                break;
            case 229: // MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                setState(RecordingProcessState.MAX_FILE_SIZE_REACHED, recordingInfo);
                break;
            case 201:
                setState(RecordingProcessState.OUTPUT_FILE_ERROR, recordingInfo);
                break;
            case 237:
            case 251: // AudioSystem::isSourceActive()
                setState(RecordingProcessState.MICROPHONE_BUSY_ERROR, recordingInfo);
                break;
            case 209:
                setState(RecordingProcessState.OPEN_GL_ERROR, recordingInfo);
                break;
            case 217:
                setState(RecordingProcessState.SECURE_SURFACE_ERROR, recordingInfo);
                break;
            case 250: // audioRecord.initCheck()
                setState(RecordingProcessState.AUDIO_CONFIG_ERROR, recordingInfo);
                break;
            case 230: // MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                // fall through - this should never happen unless user fiddles with Free version limitations
            default:
                setState(RecordingProcessState.UNKNOWN_RECORDING_ERROR, recordingInfo);
        }
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
