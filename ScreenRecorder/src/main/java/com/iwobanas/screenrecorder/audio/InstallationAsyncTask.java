package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.AsyncTask;

import com.iwobanas.screenrecorder.stats.AudioInstallationStatsAsyncTask;

public abstract class InstallationAsyncTask extends AsyncTask<Void, Void, InstallationStatus> {

    private final AudioDriver audioDriver;
    private final Context context;
    private final AudioDriverInstaller installer;
    private final Long installId;
    private final InstallationStatus initialStatus;
    private long startTimestamp;

    public InstallationAsyncTask(Context context, AudioDriver audioDriver, Long installId, InstallationStatus initialStatus) {
        this.audioDriver = audioDriver;
        this.context = context;
        this.installer = new AudioDriverInstaller(context);
        this.installId = installId;
        this.initialStatus = initialStatus;
        if (installId != null) {
            startTimestamp = System.nanoTime();
        }
    }

    public Context getContext() {
        return context;
    }

    public AudioDriverInstaller getInstaller() {
        return installer;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (installId != null) {
            new AudioInstallationStatsAsyncTask(context, installId, initialStatus).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    protected void onPostExecute(InstallationStatus installationStatus) {
        super.onPostExecute(installationStatus);
        audioDriver.setInstallationStatus(installationStatus);
        if (installId != null) {
            long time = (System.nanoTime() - startTimestamp) / 1000000l;
            new AudioInstallationStatsAsyncTask(context, installId, installationStatus, installer.getErrorDetails(), time, installer.getMountMaster(), installer.isHard()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
