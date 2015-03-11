package com.iwobanas.screenrecorder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.File;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ProjectionThreadRunner extends AbstractRecordingProcess implements IRecordingProcess {
    private static final String TAG = "scr_ProjProcessRunner";

    private Context context;
    private MediaProjection mediaProjection;
    private ProjectionThread currentThread;
    private MediaProjection.Callback mediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            mediaProjection = null;
            if (currentThread != null) {
                currentThread.stopRecording();
            }
        }
    };
    private File file;
    private Handler handler;

    public ProjectionThreadRunner(Context context) {
        super(TAG, 3000, 3000);
        this.context = context;
        handler = new Handler();
    }

    public void setProjectionData(Intent data) {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
        if (mediaProjection != null) {
            //disable callback as it doesn't work anyways
            //mediaProjection.registerCallback(mediaProjectionCallback, handler);
            if (getState() == RecordingProcessState.STARTING) {
                start(file, null);
            } else {
                setState(RecordingProcessState.READY, null);
            }
        }
    }

    public void initialize() {
        if (mediaProjection == null) {
            Log.v(TAG, "Display projection dialog");
            requestMediaProjection();
            setState(RecordingProcessState.INITIALIZING, null);
        } else {
            Log.v(TAG, "Projection already initialized");
            setState(RecordingProcessState.READY, null);
        }
    }

    @Override
    public void start(File file, String ignored) {
        this.file = file;
        setState(RecordingProcessState.STARTING, null);

        if (currentThread != null) {
            currentThread.destroy();
        }

        if (mediaProjection == null) {
            requestMediaProjection();
            return;
        }
        currentThread = new ProjectionThread(mediaProjection, context, this);
        currentThread.startRecording(file);
    }

    public void stop() {
        if (currentThread == null) {
            Log.e(TAG, "No active thread to stop!");
            return;
        }
        currentThread.stopRecording();
    }

    @Override
    public void startTimeout() {
        if (currentThread == null) {
            Log.e(TAG, "Timeout with o active thread!");
            return;
        }
        Log.w(TAG, "Start timeout");
        currentThread.startTimeout();
    }

    @Override
    public void stopTimeout() {
        if (currentThread == null) {
            Log.e(TAG, "Timeout with o active thread!");
            return;
        }
        Log.w(TAG, "Stop timeout");
        currentThread.stopTimeout();
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        setState(RecordingProcessState.DESTROYED, null);
        if (currentThread != null) {
            currentThread.destroy();
        }
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
            } catch (Exception ignore) {
            }
        }
    }

    private void requestMediaProjection() {
        Intent intent = new Intent(context, MediaProjectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
