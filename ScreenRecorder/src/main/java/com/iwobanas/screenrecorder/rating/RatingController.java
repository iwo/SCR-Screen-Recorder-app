package com.iwobanas.screenrecorder.rating;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class RatingController {

    public static final String PREFERENCES = "RatingPrefrerences";
    public static final String DISABLED = "DISABLED";
    public static final String LAST_SHOWN = "LAST_SHOWN";
    public static final String SUCCESS_COUNT = "SUCCESS_COUNT";
    private static final int MIN_SUCCESS_COUNT = 5;
    private static final long FIRST_SHOW_TIME = 1 * 24 * 60 * 60 * 1000l; // 1 day
    private static final long SHOW_INTERVAL = 7 * 24 * 60 * 60 * 1000l; // 7 days
    private Context context;
    private SharedPreferences preferences;
    private boolean disabled;
    private int successCount;
    private long lastShown;

    public RatingController(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        this.context = context;
        disabled = preferences.getBoolean(DISABLED, false);
        if (disabled) {
            return;
        }
        successCount = preferences.getInt(SUCCESS_COUNT, 0);
        lastShown = preferences.getLong(LAST_SHOWN, 0);
        if (lastShown == 0) {
            setLastShown(System.currentTimeMillis() - SHOW_INTERVAL + FIRST_SHOW_TIME);
        }
    }

    public void increaseSuccessCount() {
        if (disabled || preferences.getBoolean(DISABLED, false)) return;

        setSuccessCount(getSuccessCount() + 1);
    }

    public void resetSuccessCount() {
        if (disabled || preferences.getBoolean(DISABLED, false)) return;

        setSuccessCount(0);
    }

    public boolean shouldShow() {
        return !disabled && successCount >= MIN_SUCCESS_COUNT && System.currentTimeMillis() - getLastShown() > SHOW_INTERVAL;
    }

    public void show() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(LAST_SHOWN, System.currentTimeMillis());
        editor.commit();
        Intent intent = new Intent(context, RatingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private int getSuccessCount() {
        return successCount;
    }

    private void setSuccessCount(int successCount) {
        this.successCount = successCount;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(SUCCESS_COUNT, successCount);
        editor.commit();
    }

    private long getLastShown() {
        return lastShown;
    }

    private void setLastShown(long lastShown) {
        this.lastShown = lastShown;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(LAST_SHOWN, lastShown);
        editor.commit();
    }
}
