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
    private String fileName;

    public ProjectionThreadRunner(Context context, IRecorderService service) {
        this.context = context;
        this.service = service;
    }

    public void setProjectionData(Intent data) {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
        if (mediaProjection != null) {
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    mediaProjection = null;
                }
            }, new Handler());
        }
        start(fileName);
    }

    public void start(String fileName) {
        Log.i(TAG, "start deviceId: " + service.getDeviceId());
        this.fileName = fileName;
        if (mediaProjection == null) {
            Intent intent = new Intent(context, MediaProjectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        }
        if (currentThread != null) {
            currentThread.destroy();
        }
        currentThread = new ProjectionThread(mediaProjection, service);
        currentThread.startRecording(new File(fileName));
    }

    public void stop() {
        if (currentThread == null) {
            Log.e(TAG, "No active thread to stop!");
            return;
        }
        currentThread.stopRecording();
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        if (currentThread != null) {
            currentThread.stopRecording();
        }
    }
}
