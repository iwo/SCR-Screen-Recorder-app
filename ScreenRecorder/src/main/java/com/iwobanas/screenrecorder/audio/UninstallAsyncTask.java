package com.iwobanas.screenrecorder.audio;

import android.content.Context;

import static com.iwobanas.screenrecorder.audio.InstallationStatus.NOT_INSTALLED;
import static com.iwobanas.screenrecorder.audio.InstallationStatus.UNINSTALLING;
import static com.iwobanas.screenrecorder.audio.InstallationStatus.UNSPECIFIED;

public class UninstallAsyncTask extends InstallationAsyncTask {

    public UninstallAsyncTask(Context context, AudioDriver audioDriver, long installId) {
        super(context, audioDriver, installId, UNINSTALLING);
    }

    @Override
    protected InstallationStatus doInBackground(Void... params) {
        return getInstaller().uninstall() ? NOT_INSTALLED : UNSPECIFIED;
    }
}
