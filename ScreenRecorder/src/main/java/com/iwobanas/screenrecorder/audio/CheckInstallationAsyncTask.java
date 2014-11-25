package com.iwobanas.screenrecorder.audio;

import android.content.Context;

import com.iwobanas.screenrecorder.settings.Settings;

import static com.iwobanas.screenrecorder.audio.InstallationStatus.CHECKING;
import static com.iwobanas.screenrecorder.audio.InstallationStatus.NOT_INSTALLED;

public class CheckInstallationAsyncTask extends InstallationAsyncTask {
    public CheckInstallationAsyncTask(Context context, AudioDriver audioDriver) {
        super(context, audioDriver, null, CHECKING);
    }

    @Override
    protected InstallationStatus doInBackground(Void... params) {
        if (!Settings.getInstance().isRootFlavor()) {
            return NOT_INSTALLED;
        }
        return getInstaller().checkStatus();
    }
}
