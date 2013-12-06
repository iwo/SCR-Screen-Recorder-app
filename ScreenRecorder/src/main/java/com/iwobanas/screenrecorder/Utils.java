package com.iwobanas.screenrecorder;

import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    public static String md5(String string) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);

        for (byte b : hash) {
            int i = (b & 0xFF);
            if (i < 0x10) hex.append('0');
            hex.append(Integer.toHexString(i));
        }

        return hex.toString();
    }

    public static int getAppVersion(Context context) {
        int appVersion = -1;
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (Exception ignored) {}
        return appVersion;
    }

    public static String getAppVersionName(Context context) {
        String appVersion = "unknown";
        try {
            appVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        return appVersion;
    }

    public static boolean checkDirWritable(File dir) {
        try {
            if (!dir.exists()) {
                if (!dir.mkdirs())
                    return false;
            }
            File testFile = new File(dir, "scr_write_test.txt");
            testFile.createNewFile();
            testFile.delete();
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    public static void extractResource(Context context, int resourceId, File outputFile) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        int count;
        byte[] buffer = new byte[1024];
        while ((count = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, count);
        }
        inputStream.close();
        outputStream.close();
    }

    public static boolean isX86() {
        return Build.CPU_ABI.contains("x86");
    }

    public static boolean isArm() {
        return Build.CPU_ABI.contains("arm");
    }
}
