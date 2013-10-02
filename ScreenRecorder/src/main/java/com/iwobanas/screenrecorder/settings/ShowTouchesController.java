package com.iwobanas.screenrecorder.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.System;

public class ShowTouchesController {
    private final String SHOW_TOUCHES_SETTING = "show_touches";

    private ContentResolver contentResolver;

    public ShowTouchesController(Context context) {
        contentResolver = context.getContentResolver();
    }

    public boolean getShowTouches() {
        int setting = System.getInt(contentResolver, SHOW_TOUCHES_SETTING, 0);
        return setting == 1;
    }

    public void setShowTouches(boolean show) {
        if (getShowTouches() != show) {
            System.putInt(contentResolver, SHOW_TOUCHES_SETTING, show ? 1 : 0);
        }
    }
}
