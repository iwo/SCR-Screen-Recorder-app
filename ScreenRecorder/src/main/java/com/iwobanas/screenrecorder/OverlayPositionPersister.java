package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.WindowManager;

public class OverlayPositionPersister {
    private static final String TAG = "scr_PositionPersister";

    public static final String SCR_UI_PREFERENCES = "scr_ui";
    private static final String POSITION_X = "_POSITION_X";
    private static final String POSITION_Y = "_POSITION_Y";
    private static final String GRAVITY = "_GRAVITY";

    private String name;
    private SharedPreferences preferences;
    private WindowManager.LayoutParams layoutParams;

    public OverlayPositionPersister(Context context, String name, WindowManager.LayoutParams layoutParams) {
        this.name = name;
        this.layoutParams = layoutParams;
        preferences = context.getSharedPreferences(SCR_UI_PREFERENCES, Context.MODE_PRIVATE);
        readPosition();
    }

    private void readPosition() {
        layoutParams.x = preferences.getInt(name + POSITION_X, layoutParams.x);
        layoutParams.y = preferences.getInt(name + POSITION_Y, layoutParams.y);
        layoutParams.gravity = preferences.getInt(name + GRAVITY, layoutParams.gravity);
        Log.v(TAG, "Initializing " + name + " position " + layoutParams.x + ":" + layoutParams.y);
    }

    public void persistPosition() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(name + POSITION_X, layoutParams.x);
        editor.putInt(name + POSITION_Y, layoutParams.y);
        editor.putInt(name + GRAVITY, layoutParams.gravity);
        editor.commit();
    }
}
