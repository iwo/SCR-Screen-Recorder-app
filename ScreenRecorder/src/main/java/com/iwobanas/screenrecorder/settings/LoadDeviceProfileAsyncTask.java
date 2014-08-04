package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.Utils;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class LoadDeviceProfileAsyncTask extends AsyncTask<Void, Void, DeviceProfile> {
    private static final String TAG = "scr_LoadDeviceProfileAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/device_profile.php?";
    private static final String DEVICE_PROFILE_FILE_NAME = "device_profile.json";
    private static final long CACHE_REFRESH_MS = 7 * 24 * 60 * 60 * 1000l; // one week

    private Map<String, String> params = new HashMap<String, String>();
    private Settings settings;

    private File cacheFile;

    public LoadDeviceProfileAsyncTask(Settings settings, Context context, int appVersion, boolean appUpdated, boolean systemUpdated) {
        this.settings = settings;
        params.put("app_version", String.valueOf(appVersion));

        cacheFile = new File(context.getFilesDir(), DEVICE_PROFILE_FILE_NAME);

        DeviceProfile profile = null;

        long cacheAge = 0;
        if (cacheFile.exists()) {
            cacheAge = System.currentTimeMillis() - cacheFile.lastModified();
            profile = createProfile(loadFromCache());
            if (profile == null) {
                Log.w(TAG, "Error reading cached profile. Deleting cache file");
                if (!cacheFile.delete()) {
                    Log.e(TAG, "Error deleting cache file");
                }
            }
        }

        settings.setDeviceProfile(profile);

        if (profile != null && cacheAge < CACHE_REFRESH_MS && !appUpdated && !systemUpdated) {
            Log.v(TAG, "No need to update device profile. Cache age: " + cacheAge + "ms");
            cancel(true);
        } else {
            Log.v(TAG, "Device profile will be loaded from the network");
        }
    }

    private DeviceProfile createProfile(String jsonString) {
        if (jsonString == null || jsonString.length() == 0) {
            Log.w(TAG, "No string passed to profile");
            return null;
        }

        DeviceProfile profile;
        try {
            JSONObject json = new JSONObject(jsonString);
            profile = new DeviceProfile(json, settings.getResolutionsManager());
        } catch (JSONException e) {
            Log.w(TAG, "Error parsing profile", e);
            return null;
        }

        return profile;
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
    protected DeviceProfile doInBackground(Void... voids) {

        String jsonString = loadFromNetwork();

        DeviceProfile profile = createProfile(jsonString);

        if (profile != null) {
            saveToCache(jsonString);
        } else {
            Log.w(TAG, "Error loading remote profile");
        }

        return profile;
    }

    private void saveToCache(String json) {
        try {
            Utils.writeStringToFile(cacheFile, json);
        } catch (IOException e) {
            Log.w(TAG, "Error writing cache", e);
        }
    }

    private String loadFromCache() {
        String json = null;
        try {
            json = Utils.readFileToString(cacheFile);
        } catch (IOException e) {
            Log.w(TAG, "Error reading cache", e);
        }
        return json;
    }

    private String loadFromNetwork() {
        String url = BASE_URL;

        for (String key : params.keySet()) {
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
        return resultString;
    }

    @Override
    protected void onPostExecute(DeviceProfile profile) {
        settings.setDeviceProfile(profile);
    }
}
