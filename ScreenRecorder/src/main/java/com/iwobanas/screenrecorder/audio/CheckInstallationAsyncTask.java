package com.iwobanas.screenrecorder.audio;

import android.content.Context;

import static com.iwobanas.screenrecorder.audio.InstallationStatus.CHECKING;

public class CheckInstallationAsyncTask extends InstallationAsyncTask {
    public CheckInstallationAsyncTask(Context context, AudioDriver audioDriver) {
        super(context, audioDriver, null, CHECKING);
    }

    @Override
    protected InstallationStatus doInBackground(Void... params) {
        return getInstaller().checkStatus();
    }
}
