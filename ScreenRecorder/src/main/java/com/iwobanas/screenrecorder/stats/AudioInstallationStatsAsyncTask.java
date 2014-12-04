package com.iwobanas.screenrecorder.stats;

import android.content.Context;

import com.iwobanas.screenrecorder.audio.InstallationStatus;

public class AudioInstallationStatsAsyncTask extends StatsBaseAsyncTask {
    private static final String TAG = "scr_AudioInstallationStatsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/audio_install.php?";

    public AudioInstallationStatsAsyncTask(Context context, long timestamp, InstallationStatus status, String details, long time) {
        super(context);
        params.put("install_id", String.valueOf(timestamp));
        params.put("status", status.name());
        params.put("details", details);
        if (time > 0) {
            params.put("install_time", String.valueOf(time));
        }
    }

    @Override
    protected String getUrl() {
        return BASE_URL;
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
