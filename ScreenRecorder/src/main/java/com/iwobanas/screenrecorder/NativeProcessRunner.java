package com.iwobanas.screenrecorder;

import android.content.Context;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.*;

public class NativeProcessRunner extends AbstractRecordingProcess implements NativeProcess.OnStateChangeListener {
    private static final String TAG = "scr_NativeProcessRunner";

    Context context;

    NativeProcess process;

    public NativeProcessRunner(Context context) {
        super(TAG);
        this.context = context;
    }

    public void start(String fileName, String rotation) {
        process.startRecording(fileName, rotation);
    }

    public void stop() {
        process.stopRecording();
    }

    public void initialize() {

        if (process == null || process.isStopped()) {
            setState(RecordingProcessState.INITIALIZING, null);
            process = new NativeProcess(context, this);
            new Thread(process).start();
        } else {
            try {
                throw new IllegalStateException();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Can't initialize process in state: " + process.getState(), e);
            }
        }
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
    public void onStateChange(NativeProcess target, NativeProcess.ProcessState state, NativeProcess.ProcessState previousState, RecordingInfo recordingInfo) {
        if (target != process) {
            Log.w(TAG, "received state update from old process");
            return;
        }

        switch (state) {
            case READY:
                setState(RecordingProcessState.READY, recordingInfo);
                break;
            case STARTING:
                setState(RecordingProcessState.STARTING, recordingInfo);
                break;
            case RECORDING:
                setState(RecordingProcessState.RECORDING, recordingInfo);
                break;
            case FINISHED:
                setState(RecordingProcessState.FINISHED, recordingInfo);
                initialize();
                break;
            case CPU_NOT_SUPPORTED_ERROR:
                setState(RecordingProcessState.CPU_NOT_SUPPORTED_ERROR, null);
                break;
            case INSTALLATION_ERROR:
                setState(RecordingProcessState.INSTALLATION_ERROR, null);
                break;
            case ERROR:
                if (previousState == NativeProcess.ProcessState.RECORDING
                    || previousState == NativeProcess.ProcessState.STARTING
                    || previousState == NativeProcess.ProcessState.STOPPING
                    || previousState == NativeProcess.ProcessState.FINISHED) {
                    handleRecordingError(recordingInfo);
                    initialize();
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
            setState(RecordingProcessState.SU_ERROR, recordingInfo);
            EasyTracker.getTracker().sendEvent(ERROR, SU_ERROR, recordingInfo.exitValue == -1 ? NO_SU : SU_DENY, null);
        } else if (recordingInfo.exitValue == 127) { // command not found
            //TODO: verify installation
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
