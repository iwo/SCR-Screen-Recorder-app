package com.iwobanas.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.util.Log;

import java.io.File;

public class ProjectionThreadRunner {
    private static final String TAG = "scr_PresentationProcessRunner";


    private IRecorderService service;
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
    private String fileName;
    private Handler handler;
    private boolean started;

    public ProjectionThreadRunner(Context context, IRecorderService service) {
        this.context = context;
        this.service = service;
        handler = new Handler();
    }

    public void setProjectionData(Intent data) {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
        if (mediaProjection != null) {
            //disable callback as it doesn't work anyways
            //mediaProjection.registerCallback(mediaProjectionCallback, handler);
            if (started) {
                start(fileName);
            } else {
                service.setReady();
            }
        }
    }

    public void initialize() {
        started = false;
        if (mediaProjection == null) {
            requestMediaProjection();
        } else {
            service.setReady();
        }
    }

    public void start(String fileName) {
        Log.i(TAG, "start deviceId: " + service.getDeviceId());
        this.fileName = fileName;
        started = true;

        if (currentThread != null) {
            currentThread.destroy();
        }

        if (mediaProjection == null) {
            requestMediaProjection();
            return;
        }
        currentThread = new ProjectionThread(mediaProjection, context, service);
        currentThread.startRecording(new File(fileName));
    }

    public void stop() {
        started = false;
        if (currentThread == null) {
            Log.e(TAG, "No active thread to stop!");
            return;
        }
        currentThread.stopRecording();
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        started = false;
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
