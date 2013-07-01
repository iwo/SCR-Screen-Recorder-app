package com.iwobanas.screenrecorder;

import android.content.res.Configuration;
import android.view.Display;
import android.view.Surface;

public class Utils {

    public static int getDeviceDefaultOrientation(Display display, Configuration config) {
        int rotation = display.getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }
}
