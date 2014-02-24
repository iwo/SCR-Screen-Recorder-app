package com.iwobanas.screenrecorder;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.settings.Settings;

import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class SendStatsAsyncTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "scr_SendStatsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/?";

    private Map<String, String> params = new HashMap<String, String>();

    public SendStatsAsyncTask(String packageName, int appVersion, String deviceId, RecordingInfo recordingInfo) {
        params.put("package_name", packageName);
        params.put("app_version", String.valueOf(appVersion));
        params.put("device_id", deviceId);
        if (recordingInfo.fileName != null) {
            params.put("recording_id", new File(recordingInfo.fileName).getName());
        }
        params.put("error_code", String.valueOf(recordingInfo.exitValue));
        params.put("recording_size", String.valueOf(recordingInfo.size));
        params.put("recording_time", String.valueOf(recordingInfo.time));
        params.put("recording_fps", String.valueOf(recordingInfo.fps));
        params.put("rotation", String.valueOf(recordingInfo.rotation));
        params.put("adjusted_rotation", String.valueOf(recordingInfo.adjustedRotation));
        params.put("vertical_input", String.valueOf(recordingInfo.verticalInput));
        params.put("rotate_view", String.valueOf(recordingInfo.rotateView));
    }

    @Override
    protected void onPreExecute() {

        params.put("build_device", Build.DEVICE);
        params.put("build_board", Build.BOARD);
        params.put("build_hardware", Build.HARDWARE);
        params.put("build_model", Build.MODEL);
        params.put("build_id", Build.ID);
        params.put("build_version_sdk_int", String.valueOf(Build.VERSION.SDK_INT));
        params.put("build_version_release", Build.VERSION.RELEASE);

        Settings s = Settings.getInstance();
        params.put("audio_source", s.getAudioSource().name());
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
    }

    private String formatBoolean(boolean value) {
        return value ? "1" : "0";
    }

    @Override
    protected Void doInBackground(Void... voids) {

        AndroidHttpClient client = AndroidHttpClient.newInstance("SCR");
        String url = BASE_URL;

        for (String key: params.keySet()) {
            String vale = params.get(key);
            if (vale == null) continue;

            try {
                url += key.toLowerCase() + '=' + URLEncoder.encode(vale, "UTF-8") + "&";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 should always be supported", e);
            }
        }

        url += "request_id=" + Utils.md5(url + "SaltLakeCity");

        HttpGet get = new HttpGet(url);
        try {
            client.execute(get);
        } catch (IOException e) {
            Log.w(TAG, "HTTP GET execution error", e);
        }
        client.close();
        return null;
    }
}
