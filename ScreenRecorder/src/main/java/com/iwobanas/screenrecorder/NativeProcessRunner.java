package com.iwobanas.screenrecorder;

import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.*;

public class NativeProcessRunner implements RecorderProcess.OnStateChangeListener {
    private static final String TAG = "scr_NativeProcessRunner";

    IRecorderService service;

    RecorderProcess process;

    private String executable;

    private String fileName;

    public NativeProcessRunner(IRecorderService service) {
        this.service = service;
    }

    public void start(String fileName, String rotation) {
        this.fileName = fileName;
        Log.i(TAG, "start deviceId: " + service.getDeviceId());
        process.startRecording(fileName, rotation);
    }

    public void stop() {
        process.stopRecording();
    }

    public void initialize() {
        if (process == null || process.isStopped()) {
            process = new RecorderProcess(executable, this);
            this.fileName = null;
            new Thread(process).start();
        }
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        if (process == null || process.isStopped()) {
            return;
        }

        if (process.isRecording()) {
            process.stopRecording();
        } else {
            process.destroy();
        }
    }

    @Override
    public void onStateChange(RecorderProcess target, RecorderProcess.ProcessState state, RecorderProcess.ProcessState previousState, int exitValue, float fps) {
        if (target != process) {
            Log.w(TAG, "received state update from old process");
            return;
        }
        switch (state) {
            case READY:
                service.setReady(true);
                break;
            case FINISHED:
                service.recordingFinished(fps);
                service.setReady(false);
                break;
            case ERROR:
                if (previousState == RecorderProcess.ProcessState.RECORDING
                    || previousState == RecorderProcess.ProcessState.STOPPING
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
        if (exitValue == -1 || exitValue == 1) { // general error e.g. SuperSu Deny access
            Log.e(TAG, "Error code 1. Assuming no super user access");
            service.suRequired();
            EasyTracker.getTracker().sendEvent(ERROR, SU_ERROR, exitValue == -1 ? NO_SU : SU_DENY, null);
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
        switch (exitValue) {
            case 302: // start timeout
            case 213: // start() error
            case 227: // MEDIA_RECORDER_EVENT_ERROR
            case 228: // MEDIA_RECORDER_TRACK_EVENT_ERROR
            case 248: // MEDIA_RECORDER_EVENT_ERROR during startup
            case 249: // MEDIA_RECORDER_TRACK_EVENT_ERROR during startup
            case 198: // SurfaceMediaSource error
                service.mediaRecorderError(exitValue);
                break;
            case 229: // MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                service.maxFileSizeReached(exitValue);
                break;
            case 201:
                service.outputFileError(exitValue);
                break;
            case 237:
            case 251: // AudioSystem::isSourceActive()
                service.microphoneBusyError(exitValue);
                break;
            case 209:
                service.openGlError(exitValue);
                break;
            case 217:
                service.secureSurfaceError(exitValue);
                break;
            case 250: // audioRecord.initCheck()
                service.audioConfigError(exitValue);
                break;
            case 230: // MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                // fall through - this should never happen unless user fiddles with Free version limitations
            default:
                service.recordingError(exitValue);
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
