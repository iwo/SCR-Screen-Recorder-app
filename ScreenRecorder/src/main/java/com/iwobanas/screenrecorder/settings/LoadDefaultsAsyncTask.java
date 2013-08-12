package com.iwobanas.screenrecorder.settings;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LoadDefaultsAsyncTask extends AsyncTask<Void, Void, Properties> {
    private static final String TAG = "scr_GetDefaultSettingsAsyncTask";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/defaults.php?";

    private Map<String, String> params = new HashMap<String, String>();

    public LoadDefaultsAsyncTask() {
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
    protected Properties doInBackground(Void... voids) {
        Properties properties = null;
        AndroidHttpClient client = AndroidHttpClient.newInstance("SCR");
        String url = BASE_URL;

        for (String key: params.keySet()) {
            try {
                url += key.toLowerCase() + '=' + URLEncoder.encode(params.get(key), "UTF-8") + "&";
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 should always be supported", e);
            }
        }

        HttpGet get = new HttpGet(url);
        ResponseHandler<String> handler = new BasicResponseHandler();
        String result;
        try {
            result = client.execute(get, handler);
            properties = new Properties();
            properties.load(new StringReader(result));
        } catch (IOException e) {
            Log.w(TAG, "HTTP GET execution error", e);
        }
        client.close();
        return properties;
    }

    @Override
    protected void onPostExecute(Properties properties) {
        Settings.getInstance().updateDefaults(
            properties.getProperty("resolution_width"),
            properties.getProperty("resolution_height"),
            properties.getProperty("transformation"),
            properties.getProperty("color_fix")
        );
    }
}
