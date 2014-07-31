package com.iwobanas.screenrecorder.settings;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class LoadDefaultsAsyncTask extends AsyncTask<Void, Void, JSONObject> {
    private static final String TAG = "scr_GetDefaultSettingsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/device_profile.php?";

    private Map<String, String> params = new HashMap<String, String>();

    public LoadDefaultsAsyncTask(int appVersion) {
        params.put("app_version", String.valueOf(appVersion));
    }

    @Override
    protected void onPreExecute() {
        params.put("build_device", Build.DEVICE);
        params.put("build_board", Build.BOARD);
        params.put("build_hardware", Build.HARDWARE);
        params.put("build_id", Build.ID);
        params.put("build_version_sdk_int", String.valueOf(Build.VERSION.SDK_INT));
        params.put("build_version_release", Build.VERSION.RELEASE);
    }

    @Override
    protected JSONObject doInBackground(Void... voids) {

        String url = BASE_URL;

        for (String key: params.keySet()) {
            try {
                url += key.toLowerCase() + '=' + URLEncoder.encode(params.get(key), "UTF-8") + "&";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 should always be supported", e);
            }
        }

        String resultString;
        try {
            HttpGet get = new HttpGet(url);
            ResponseHandler<String> handler = new BasicResponseHandler();
            AndroidHttpClient client = AndroidHttpClient.newInstance("SCR");
            resultString = client.execute(get, handler);
            client.close();

        } catch (Exception e) {
            Log.w(TAG, "HTTP GET execution error", e);
            return null;
        }

        JSONObject defaults = null;
        try {
            JSONObject profile = new JSONObject(resultString);
            defaults = profile.getJSONObject("defaults");
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing profile", e);
        }

        return defaults;
    }

    @Override
    protected void onPostExecute(JSONObject defaults) {
        if (defaults == null) return;

        try {
            Settings.getInstance().updateDefaults(
                defaults.has("resolution_width") ? defaults.getString("resolution_width"): null,
                defaults.has("resolution_height") ? defaults.getString("resolution_height"): null,
                defaults.has("transformation") ? defaults.getString("transformation"): null,
                defaults.has("video_bitrate") ? defaults.getString("video_bitrate"): null,
                defaults.has("sampling_rate") ? defaults.getString("sampling_rate"): null,
                defaults.has("color_fix") ? defaults.getString("color_fix"): null,
                defaults.has("video_encoder") ? defaults.getString("video_encoder"): null
            );
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing default settings", e);
        }
    }
}
