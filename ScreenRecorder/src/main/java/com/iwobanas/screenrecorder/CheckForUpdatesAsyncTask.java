package com.iwobanas.screenrecorder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class CheckForUpdatesAsyncTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "scr_CheckUpdatesAT";
    private static final String CURRENT_VERSION_KEY = "current_version";
    private static final String LAST_NOTIFICATION_MILLIS_KEY = "last_notification_millis";
    private static final String LAST_CHECK_MILLIS_KEY = "last_check_millis";
    private static final int UPDATE_NOTIFICATION_ID = 3;
    private static final int HOUR_TO_MILLIS = 60 * 60 * 1000;

    private final Context context;

    public CheckForUpdatesAsyncTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected String doInBackground(Void... params) {
        SharedPreferences preferences = context.getSharedPreferences("updates", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long lastNotification = preferences.getLong(LAST_NOTIFICATION_MILLIS_KEY, 0);
        if (now - lastNotification < 8 * HOUR_TO_MILLIS) {
            return null;
        }
        long lastCheck = preferences.getLong(LAST_CHECK_MILLIS_KEY, 0);
        int installedVersion = Utils.getAppVersion(context);
        int currentVersion = preferences.getInt(CURRENT_VERSION_KEY, 0);

        if (currentVersion <= 0 || now - lastCheck > 24 * HOUR_TO_MILLIS) {
            currentVersion = getCurrentVersion();
            if (currentVersion > 0) {
                preferences.edit()
                        .putInt(CURRENT_VERSION_KEY, currentVersion)
                        .putLong(LAST_CHECK_MILLIS_KEY, now)
                        .commit();
            }
        }

        if (installedVersion > 0 && installedVersion < currentVersion) {
            preferences.edit()
                    .putLong(LAST_NOTIFICATION_MILLIS_KEY, now)
                    .commit();
            return convertToVersionName(currentVersion);
        }

        return null;
    }

    private int getCurrentVersion() {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL("http://www.scr-screen-recorder.com/current_version/" + context.getPackageName() + ".txt");
        } catch (MalformedURLException e) {
            return -1;
        }
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String response = reader.readLine();
                return Integer.parseInt(response);
            } else {
                Log.w(TAG, "Unexpected HTTP response: " + responseCode);
            }
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "Error fetching version", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return 0;
    }

    private String convertToVersionName(int version) {
        int major = version / 10000;
        int minor = (version / 100) % 100;
        int patch = version % 100;
        return major + "." + minor + "." + patch;
    }

    @Override
    protected void onPostExecute(String newVersion) {
        if (newVersion != null) {
            String title = context.getString(R.string.update_notification_title, context.getString(R.string.app_name), newVersion);
            String text = context.getString(R.string.update_notification_text);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setAutoCancel(true)
                    .setContentText(text);

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.setData(Uri.parse("http://www.scr-screen-recorder.com"));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendingIntent);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            try {
                notificationManager.notify(UPDATE_NOTIFICATION_ID, builder.build());
            } catch (SecurityException e) {
                // Android 4.1.2 issue
                // could be fixed by adding <uses-permission android:name="android.permission.WAKE_LOCK" />
                Log.w(TAG, "Couldn't display notification", e);
            }
        }
    }
}
