package com.iwobanas.screenrecorder.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings.System;
import android.util.Log;

public class ShowTouchesController {
    private static final String PREFERENCES_NAME = "ShowTouchesPreferences";
    private static final String SYSTEM_VALUE = "SYSTEM_VALUE";
    private static final String SCR_VALUE_APPLIED = "SCR_VALUE_APPLIED";
    private final String SHOW_TOUCHES_SETTING = "show_touches";
    private final String TAG = "scr_ShowTouchesController";
    private ContentResolver contentResolver;
    private SharedPreferences preferences;
    private boolean scrValue = true;
    private boolean systemValue = false;
    private Boolean changing = false;

    public ShowTouchesController(Context context) {
        contentResolver = context.getContentResolver();
        contentResolver.registerContentObserver(System.CONTENT_URI, true, new SystemSettingsObserver());
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (preferences.getBoolean(SCR_VALUE_APPLIED, false)) {
            systemValue = preferences.getBoolean(SYSTEM_VALUE, false);
            Log.w(TAG, "System value not restored on last shutdown! Using cached: " + systemValue);
        } else {
            systemValue = getSystemSetting();
            Log.d(TAG, "Initial system value set to: " + systemValue);
            setBooleanPreference(SYSTEM_VALUE, systemValue);
        }
    }

    public void setShowTouches(boolean show) {
        Log.d(TAG, "set " + show);
        scrValue = show;
        applyScrValue();
    }

    public void applyShowTouches() {
        Log.d(TAG, "apply " + scrValue);
        applyScrValue();
    }

    public void restoreShowTouches() {
        Log.d(TAG, "restore " + systemValue);
        applySystemValue();
    }

    private void applyScrValue() {
        setBooleanPreference(SCR_VALUE_APPLIED, true);
        setSystemSetting(scrValue);
    }

    private void applySystemValue() {
        setSystemSetting(systemValue);
        setBooleanPreference(SCR_VALUE_APPLIED, false);
    }

    private boolean getSystemSetting() {
        int setting = System.getInt(contentResolver, SHOW_TOUCHES_SETTING, 0);
        return setting == 1;
    }

    private void setSystemSetting(boolean show) {
        if (getSystemSetting() == show)
            return;


        synchronized (this) {
            changing = true;
            System.putInt(contentResolver, SHOW_TOUCHES_SETTING, show ? 1 : 0);
            changing = false;
        }
    }

    private void setBooleanPreference(String name, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(name, value);
        editor.commit();
    }

    class SystemSettingsObserver extends ContentObserver {

        public SystemSettingsObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);

        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null && !SHOW_TOUCHES_SETTING.equals(uri.getLastPathSegment())) return;
            Log.d(TAG, "system settings changed");
            synchronized (ShowTouchesController.this) {
                if (changing) {
                    Log.d(TAG, "ignoring local change");
                    return;
                }
                boolean currentValue = getSystemSetting();
                boolean scrValueApplied = preferences.getBoolean(SCR_VALUE_APPLIED, false);

                if ((scrValueApplied && currentValue != scrValue) ||
                        (!scrValueApplied && currentValue != systemValue)) {
                    Log.d(TAG, "Show touches changed from outside SCR. value: " + currentValue + " SCR foreground: " + scrValueApplied);
                    systemValue = currentValue;
                    setBooleanPreference(SYSTEM_VALUE, systemValue);
                    setBooleanPreference(SCR_VALUE_APPLIED, false);
                } else {
                    if (uri == null) {
                        Log.d(TAG, "ignoring unrelated setting change");
                    } else {
                        Log.w(TAG, "change callback executed but no change detected. value: " + currentValue + " SCR foreground: " + scrValueApplied);
                    }
                }
            }
        }
    }
}
