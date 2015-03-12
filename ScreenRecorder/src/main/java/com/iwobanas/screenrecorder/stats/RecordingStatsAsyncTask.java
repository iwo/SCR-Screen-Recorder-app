package com.iwobanas.screenrecorder.stats;

import android.content.Context;

import com.iwobanas.screenrecorder.RecordingInfo;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.audio.InstallationStatus;
import com.iwobanas.screenrecorder.settings.Settings;

public class RecordingStatsAsyncTask extends StatsBaseAsyncTask {
    private static final String TAG = "scr_SendStatsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/?";

    public RecordingStatsAsyncTask(Context context, RecordingInfo recordingInfo) {
        super(context);
        if (recordingInfo.file != null) {
            params.put("recording_id", recordingInfo.file.getName());
        }
        int timeLapse = Settings.getInstance().getTimeLapse();
        params.put("error_code", String.valueOf(recordingInfo.exitValue));
        params.put("recording_size", String.valueOf(recordingInfo.size));
        params.put("recording_time", String.valueOf(recordingInfo.time / timeLapse));
        params.put("recording_fps", String.valueOf(recordingInfo.fps * timeLapse));
        params.put("rotation", String.valueOf(recordingInfo.rotation));
        params.put("adjusted_rotation", String.valueOf(recordingInfo.adjustedRotation));
        params.put("vertical_input", String.valueOf(recordingInfo.verticalInput));
        params.put("rotate_view", String.valueOf(recordingInfo.rotateView));
        params.put("validity", recordingInfo.formatValidity.getCode());
        params.put("front_camera", formatBoolean(Utils.hasFrontFacingCamera(context)));
        params.put("time_lapse", String.valueOf(timeLapse));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Settings s = Settings.getInstance();
        String audioSource = s.getAudioSource().name();
        if (s.getAudioSource().getRequiresDriver()
                && s.getAudioDriver().getInstallationStatus() != InstallationStatus.INSTALLED) {
            audioSource = "ERROR";
        }
        params.put("audio_source", audioSource);
        params.put("resolution_width", String.valueOf(s.getResolution().getWidth()));
        params.put("resolution_height", String.valueOf(s.getResolution().getHeight()));
        params.put("frame_rate", String.valueOf(s.getFrameRate()));
        params.put("transformation", s.getTransformation().name());
        params.put("video_bitrate", s.getVideoBitrate().getCommand());
        params.put("sampling_rate", s.getSamplingRate().getCommand());
        params.put("color_fix", formatBoolean(s.getColorFix()));
        params.put("video_encoder", String.valueOf(s.getVideoEncoder()));
        params.put("vertical_frames", formatBoolean(s.getVerticalFrames()));
        params.put("defaults_all", formatBoolean(s.currentEqualsDefault()));
        params.put("defaults_core", formatBoolean(s.coreEqualsDefault()));
        params.put("defaults_stats", formatBoolean(s.statsBasedDefaults()));
        params.put("settings_modified", formatBoolean(s.areSettingsModified()));
        params.put("camera_view", formatBoolean(s.getShowCamera()));
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
