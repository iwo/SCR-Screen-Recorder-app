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
