package com.iwobanas.screenrecorder.stats;

import android.content.Context;

import com.iwobanas.screenrecorder.NativeCommands;
import com.iwobanas.screenrecorder.audio.InstallationStatus;

public class AudioInstallationStatsAsyncTask extends StatsBaseAsyncTask {
    private static final String TAG = "scr_AudioInstallationStatsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/audio_install.php?";

    public AudioInstallationStatsAsyncTask(Context context, long timestamp, InstallationStatus status) {
        this(context, timestamp, status, null, 0, null);

    }

    public AudioInstallationStatsAsyncTask(Context context, long timestamp, InstallationStatus status, String details, long time, Boolean mountMaster) {
        super(context);
        params.put("install_id", String.valueOf(timestamp));
        params.put("status", status.name());
        params.put("details", details);
        if (time > 0) {
            params.put("install_time", String.valueOf(time));
        }
        if (mountMaster != null) {
            params.put("mount_master", formatBoolean(mountMaster));
        }
        params.put("exec_blocked", formatBoolean(NativeCommands.getInstance().isExecBlocked()));
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
