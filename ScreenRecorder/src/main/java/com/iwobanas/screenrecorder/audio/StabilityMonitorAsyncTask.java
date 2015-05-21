package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.stats.AudioInstallationStatsAsyncTask;

public class StabilityMonitorAsyncTask extends AsyncTask<Void, Void, Boolean>{
    private static final String TAG = "scr_StabilityMonitor";
    private static final long MAX_EXEC_TIME = 20000000000l; // 20s
    private static final int MAX_RESTARTS = 2;
    private static final String MEDIA_SERVER_COMMAND = "/system/bin/mediaserver";

    private final Context context;
    private final Long installId;
    private final AudioDriver audioDriver;
    private long startTime;

    public StabilityMonitorAsyncTask(Context context, AudioDriver audioDriver, Long installId) {
        this.audioDriver = audioDriver;
        this.context = context;
        this.installId = installId;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Log.v(TAG, "Starting");
        startTime = System.nanoTime();
        int processNumber = 0;

        while (shouldContinue()) {

            int pid = getPid();
            if (pid > 0) {
                processNumber++;
                Log.v(TAG, "New process found");
                while (Utils.processExists(pid) && shouldContinue()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                }
                if (shouldContinue()) {
                    Log.v(TAG, "Process died");
                    if (processNumber >= MAX_RESTARTS) {
                        Log.w(TAG, "Detected " + processNumber + " restarts.");
                        return false;
                    }
                }
            } else if (!isCancelled()) {
                Log.w(TAG, "No process found");
                return false;
            }
        }
        return true;
    }

    private int getPid() {
        int pid = Utils.findProcessByCommand(MEDIA_SERVER_COMMAND);
        while (pid < 1 && shouldContinue()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            pid = Utils.findProcessByCommand(MEDIA_SERVER_COMMAND);
        }
        return pid;
    }

    private boolean shouldContinue() {
        return !isCancelled() && System.nanoTime() - startTime < MAX_EXEC_TIME;
    }

    @Override
    protected void onPostExecute(Boolean stable) {
        Log.v(TAG, "Completed");
        if (stable != null && !stable) {
            new AudioInstallationStatsAsyncTask(context, installId, InstallationStatus.UNSTABLE).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            audioDriver.setInstallationStatus(InstallationStatus.UNSTABLE);
        }
    }

    @Override
    protected void onCancelled() {
        Log.v(TAG, "Cancelled");
        super.onCancelled();
    }
}
