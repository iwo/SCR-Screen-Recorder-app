package com.iwobanas.screenrecorder;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.*;

public class NativeProcessRunner implements RecorderProcess.OnStateChangeListener {
    private static final String TAG = "scr_NativeProcessRunner";

    IRecorderService service;

    Context context;

    RecorderProcess process;

    private String executable;

    public NativeProcessRunner(Context context, IRecorderService service) {
        this.context = context;
        this.service = service;
    }

    public void start(String fileName, String rotation) {
        Log.i(TAG, "start deviceId: " + service.getDeviceId());
        process.startRecording(fileName, rotation);
    }

    public void stop() {
        process.stopRecording();
    }

    public void initialize() {
        if (executable == null) {
            return;
        }

        if (process == null || process.isStopped()) {
            process = new RecorderProcess(context, executable, this);
            new Thread(process).start();
        } else {
            try {
                throw new IllegalStateException();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Can't initialize process in state: " + process.getState(), e);
            }
        }
    }

    public void initialize(String executable) {
        this.executable = executable;
        initialize();
    }

    public boolean isReady() {
        return process != null && process.getState() == RecorderProcess.ProcessState.READY;
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
    public void onStateChange(RecorderProcess target, RecorderProcess.ProcessState state, RecorderProcess.ProcessState previousState, RecordingInfo recordingInfo) {
        if (target != process) {
            Log.w(TAG, "received state update from old process");
            return;
        }
        switch (state) {
            case READY:
                service.setReady();
                break;
            case RECORDING:
                service.recordingStarted();
                break;
            case FINISHED:
                service.recordingFinished(recordingInfo);
                break;
            case ERROR:
                if (previousState == RecorderProcess.ProcessState.RECORDING
                    || previousState == RecorderProcess.ProcessState.STARTING
                    || previousState == RecorderProcess.ProcessState.STOPPING
                    || previousState == RecorderProcess.ProcessState.FINISHED) {
                    handleRecordingError(recordingInfo);
                } else {
                    handleStartupError(recordingInfo);
                }
                break;
            default:
                break;
        }
    }

    private void handleStartupError(RecordingInfo recordingInfo) {
        if (recordingInfo.exitValue == -1 || recordingInfo.exitValue == 1 || recordingInfo.exitValue == 255) { // general error e.g. SuperSu Deny access
            Log.e(TAG, "Error code 1. Assuming no super user access");
            service.suRequired();
            EasyTracker.getTracker().sendEvent(ERROR, SU_ERROR, recordingInfo.exitValue == -1 ? NO_SU : SU_DENY, null);
        } else if (recordingInfo.exitValue == 127) { // command not found
            //TODO: verify installation
            Log.e(TAG, "Error code 127. This may be an installation issue");
            service.startupError(recordingInfo);
        } else {
            logError(recordingInfo.exitValue);
            service.startupError(recordingInfo);
        }
    }

    private void handleRecordingError(RecordingInfo recordingInfo) {
        logError(recordingInfo.exitValue);
        switch (recordingInfo.exitValue) {
            case 302: // start timeout
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    service.selinuxError(recordingInfo);
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
                service.mediaRecorderError(recordingInfo);
                break;
            case 229: // MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                service.maxFileSizeReached(recordingInfo);
                break;
            case 201:
                service.outputFileError(recordingInfo);
                break;
            case 237:
            case 251: // AudioSystem::isSourceActive()
                service.microphoneBusyError(recordingInfo);
                break;
            case 209:
                service.openGlError(recordingInfo);
                break;
            case 217:
                service.secureSurfaceError(recordingInfo);
                break;
            case 250: // audioRecord.initCheck()
                service.audioConfigError(recordingInfo);
                break;
            case 230: // MEDIA_RECORDER_INFO_MAX_DURATION_REACHED
                // fall through - this should never happen unless user fiddles with Free version limitations
            default:
                service.recordingError(recordingInfo);
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
