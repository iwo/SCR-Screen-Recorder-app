package com.iwobanas.screenrecorder.audio;

import android.content.Context;

public class CheckInstallationAsyncTask extends InstallationAsyncTask {
    public CheckInstallationAsyncTask(Context context, AudioDriver audioDriver) {
        super(context, audioDriver);
    }

    @Override
    protected InstallationStatus doInBackground(Void... params) {
        return getInstaller().checkStatus();
    }
}
