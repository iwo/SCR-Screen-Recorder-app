package com.iwobanas.screenrecorder;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.settings.Settings;

import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class SendStatsAsyncTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "scr_SendStatsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/?";

    private Map<String, String> params = new HashMap<String, String>();

    public SendStatsAsyncTask(String packageName, int appVersion, String deviceId, String recordingId, int errorCode, long size, long time) {
        params.put("package_name", packageName);
        params.put("app_version", String.valueOf(appVersion));
        params.put("device_id", deviceId);
        params.put("recording_id", recordingId);
        params.put("error_code", String.valueOf(errorCode));
        params.put("recording_size", String.valueOf(size));
        params.put("recording_time", String.valueOf(time));
    }

    @Override
    protected void onPreExecute() {

        params.put("build_device", Build.DEVICE);
        params.put("build_board", Build.BOARD);
        params.put("build_hardware", Build.HARDWARE);
        params.put("build_id", Build.ID);
        params.put("build_version_sdk_int", String.valueOf(Build.VERSION.SDK_INT));
        params.put("build_version_release", Build.VERSION.RELEASE);

        Settings s = Settings.getInstance();
        params.put("audio_source", s.getAudioSource().name());
        params.put("resolution_width", String.valueOf(s.getResolution().getWidth()));
        params.put("resolution_height", String.valueOf(s.getResolution().getHeight()));
        params.put("frame_rate", String.valueOf(s.getFrameRate()));
        params.put("transformation", s.getTransformation().name());
        params.put("color_fix", s.getColorFix() ? "1" : "0");
    }

    @Override
    protected Void doInBackground(Void... voids) {

        AndroidHttpClient client = AndroidHttpClient.newInstance("SCR");
        String url = BASE_URL;

        for (String key: params.keySet()) {
            try {
                url += key.toLowerCase() + '=' + URLEncoder.encode(params.get(key), "UTF-8") + "&";
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
