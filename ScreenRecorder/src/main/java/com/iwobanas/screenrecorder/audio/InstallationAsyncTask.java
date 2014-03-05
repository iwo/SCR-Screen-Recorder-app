package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.AsyncTask;

public abstract class InstallationAsyncTask extends AsyncTask<Void, Void, InstallationStatus> {

    private final AudioDriver audioDriver;
    private final Context context;
    private final AudioDriverInstaller installer;

    public InstallationAsyncTask(Context context, AudioDriver audioDriver) {
        this.audioDriver = audioDriver;
        this.context = context;
        this.installer = new AudioDriverInstaller(context);
    }

    public Context getContext() {
        return context;
    }

    public AudioDriverInstaller getInstaller() {
        return installer;
    }

    @Override
    protected void onPostExecute(InstallationStatus installationStatus) {
        super.onPostExecute(installationStatus);
        audioDriver.setInstallationStatus(installationStatus);
    }
}
