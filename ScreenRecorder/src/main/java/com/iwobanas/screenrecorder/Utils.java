package com.iwobanas.screenrecorder;

import android.content.res.Configuration;
import android.view.Display;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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

    public static int findProcessByCommand(String command) {
        try {
            File proc = new File("/proc");
            for (String pid : proc.list()) {
                if (!pid.matches("^[0-9]+$"))
                    continue;

                String cmdline = null;
                try {
                    BufferedReader cmdReader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
                    cmdline = cmdReader.readLine();
                } catch (FileNotFoundException ignored) {
                } catch (IOException ignored) {}


                if (cmdline != null && cmdline.startsWith(command)) {
                    return Integer.parseInt(pid);
                }
            }
        } catch (SecurityException ignored) {}
        return -1;
    }
}
