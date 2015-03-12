package com.iwobanas.screenrecorder;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String TAG = "scr_Utils";

    public static int findProcessByCommand(String command) {
        try {
            File proc = new File("/proc");

            ByteBuffer cmdByteBuffer = ByteBuffer.allocate(command.length());
            cmdByteBuffer.put(command.getBytes());
            ByteBuffer procByteBuffer = ByteBuffer.allocate(command.length());
            int length = command.length();

            FilenameFilter pidFilter = new FilenameFilter() {
                Matcher matcher = Pattern.compile("^\\d+$").matcher("");

                @Override
                public boolean accept(File dir, String filename) {
                    matcher.reset(filename);
                    return matcher.matches();
                }
            };

            String[] pidStrings = proc.list(pidFilter);
            List<Integer> processIds = new ArrayList<Integer>(pidStrings.length);
            for (int i = 0; i < pidStrings.length; i++) {
                try {
                    processIds.add(i, Integer.valueOf(pidStrings[i]));
                } catch (NumberFormatException ignore) {
                }
            }
            Collections.sort(processIds, Collections.reverseOrder());

            for (int pid : processIds) {
                FileInputStream inputStream = null;
                FileChannel fileChannel = null;
                try {
                    inputStream = new FileInputStream("/proc/" + pid + "/cmdline");
                    fileChannel = inputStream.getChannel();
                    procByteBuffer.rewind();
                    if (fileChannel.read(procByteBuffer) != length) {
                        continue;
                    }

                    cmdByteBuffer.rewind();
                    procByteBuffer.rewind();

                    if (cmdByteBuffer.compareTo(procByteBuffer) == 0) {
                        return pid;
                    }

                } catch (FileNotFoundException ignored) {
                } catch (IOException ignored) {
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (fileChannel != null) {
                            fileChannel.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

    public static boolean isMiUi(Context context) {
        String miuiVersion = getSystemProperty(context, "ro.miui.ui.version.code", "");
        return miuiVersion != null && miuiVersion.length() > 0;
    }

    public static String getSystemProperty(Context context, String key, String def) {
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class<?> SystemProperties = classLoader.loadClass("android.os.SystemProperties");
            Class[] paramTypes= new Class[2];
            paramTypes[0]= String.class;
            paramTypes[1]= String.class;
            Method get = SystemProperties.getMethod("get", paramTypes);

            return (String) get.invoke(SystemProperties, key, def);
        } catch (Exception e) {
            Log.w(TAG, "Can't access system property: " + key, e);
        }
        return def;
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
        if (resourceFileValid(context, resourceId, outputFile))
            return;
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

    public static boolean resourceFileValid(Context context, int resourceId, File outputFile) throws IOException {
        if (!outputFile.exists()) {
            return false;
        }
        InputStream resourceStream = new BufferedInputStream(context.getResources().openRawResource(resourceId));
        InputStream fileStream = null;
        try {
            fileStream = new BufferedInputStream(new FileInputStream(outputFile));
        } catch (IOException e) {
            return false;
        }

        return streamsEqual(resourceStream, fileStream);
    }

    private static boolean streamsEqual(InputStream i1, InputStream i2) throws IOException {
        byte[] buf1 = new byte[1024];
        byte[] buf2 = new byte[1024];
        try {
            DataInputStream d2 = new DataInputStream(i2);
            int len;
            while ((len = i1.read(buf1)) > 0) {
                d2.readFully(buf2,0,len);
                for(int i=0;i<len;i++)
                    if(buf1[i] != buf2[i]) return false;
            }
            return d2.read() < 0; // is the end of the second file also.
        } catch(EOFException ioe) {
            return false;
        } finally {
            i1.close();
            i2.close();
        }
    }

    public static boolean filesEqual(File fileA, File fileB) {
        return filesEqual(fileA, fileB, false);
    }

    public static boolean filesEqual(File fileA, File fileB, boolean ignorePermissions) {
        if (!fileA.exists() || !fileB.exists() || fileA.length() != fileB.length()) {
            return false;
        }
        InputStream streamA, streamB;
        try {
            streamA = new BufferedInputStream(new FileInputStream(fileA));
            streamB = new BufferedInputStream(new FileInputStream(fileB));
            return streamsEqual(streamA, streamB);
        } catch (IOException e) {
            return  (ignorePermissions && (!fileA.canRead() || !fileB.canRead()));
        }
    }

    public static boolean isX86() {
        return Build.CPU_ABI.contains("x86");
    }

    public static boolean isArm() {
        return Build.CPU_ABI.contains("arm");
    }

    public static boolean hasFrontFacingCamera(Context context) {
        PackageManager pm = context.getPackageManager();
        return  pm != null && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public static boolean copyFile(File sourceFile, File destFile) {
        return copyFile(sourceFile, destFile, false);
    }

    public static boolean copyFile(File sourceFile, File destFile, boolean ignorePermissions) {

        FileChannel source = null;
        FileChannel destination = null;
        boolean success = false;

        try {
            if(!destFile.exists()) {
                destFile.createNewFile();
            }
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            success = true;
        } catch (IOException e) {
            if ((ignorePermissions && !sourceFile.canRead())) {
                Log.v(TAG, "Error copying file", e);
                success = true;
            } else {
                success = false;
                Log.e(TAG, "Error copying file", e);
            }
        } finally {
            if(source != null) {
                try {
                    source.close();
                } catch (IOException ignored) {}
            }
            if(destination != null) {
                try {
                    destination.close();
                } catch (IOException ignored) {}
            }
        }
        return success;
    }

    public static boolean allFilesCopied(File sourceDir, File destinationDir, boolean ignorePermissions) {
        String[] fileNames = sourceDir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                File src = new File(sourceDir, fileName);
                File dst = new File(destinationDir, fileName);
                if ((src.isDirectory() && !allFilesCopied(src, dst, ignorePermissions)) || !filesEqual(src, dst, ignorePermissions)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean deleteDir(File dir) {
        ShellCommand cmd = new ShellCommand(new String[]{"rm", "-rf", dir.getAbsolutePath()});
        cmd.setErrorLogTag("scr_deleteDir_error");
        cmd.execute();
        return (cmd.isExecutionCompleted() && cmd.exitValue() == 0) || recursiveDelete(dir);
    }

    private static boolean recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!recursiveDelete(f))
                        return false;
                }
            }
        }
        return file.delete();
    }

    public static boolean copyDir(File sourceDir, File destinationDir, boolean ignorePermissions) {
        if (!destinationDir.exists()) {
            if (!destinationDir.mkdirs()) {
                Log.w(TAG, "Can't create directory: " + destinationDir);
                return false;
            }
        }
        String[] fileNames = sourceDir.list();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                File src = new File(sourceDir, fileName);
                File dst = new File(destinationDir, fileName);
                if ((src.isDirectory() && !copyDir(src, dst, ignorePermissions)) || !copyFile(src, dst, ignorePermissions)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void setGlobalReadable(File file) {
        file.setReadable(true, false);
        if (file.isDirectory()) {
            file.setExecutable(true, false);
            String[] children = file.list();
            if (children != null) {
                for (String child : children) {
                    setGlobalReadable(new File(file, child));
                }
            }
        }
    }

    public static List<String> grepFile(File file, String substring) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> result = new ArrayList<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains(substring)) {
                result.add(line);
            }
        }
        reader.close();
        return result;
    }

    public static void logDirectoryListing(String tag, File dir) {
        Log.v(tag, "Directory listing for: " + dir.getAbsolutePath());
        logDirectoryListing(tag, dir, "");
    }

    private static void logDirectoryListing(String tag, File dir, String prefix) {
        String[] children = dir.list();
        if (children == null) {
            Log.v(tag, prefix + " [empty]");
        } else {
            Arrays.sort(children);
            for (String childName : children) {
                File child = new File(dir, childName);
                if (child.isDirectory()) {
                    logDirectoryListing(tag, child, prefix + childName + "/" );
                } else {
                    Log.v(tag, prefix + childName);
                }
            }
        }
    }

    public static void logFileContent(String tag, File file) {
        Log.v(tag, "File contents: " + file.getAbsolutePath());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null) {
                Log.v(tag, line);
            }
            reader.close();
        } catch (IOException e) {
            Log.v(tag, "Error reading file", e);
        }

    }

    public static boolean processExists(int pid) {
        File procFile = new File("/proc", String.valueOf(pid));
        return procFile.exists();
    }

    public static Intent findSuIntent(Context context) {
        String[] names = new String[]{
                "eu.chainfire.supersu/.MainActivity",
                "com.android.settings/.cyanogenmod.superuser.MainActivity",
                "com.koushikdutta.superuser/.MainActivity",
                "com.mgyun.shua.su/.ui.SplashActivity",
                "com.baidu.easyroot/.SplashActivity",
                "com.noshufou.android.su/.Su",
                "com.noshufou.android.su.elite/.Su",
                "com.noshufou.android.su.elite/com.noshufou.android.su.Su"
        };

        PackageManager pm = context.getPackageManager();
        try {
            for (String name : names) {
                ComponentName componentName = ComponentName.unflattenFromString(name);
                Intent intent = Intent.makeMainActivity(componentName);
                List<ResolveInfo> activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (activities != null && activities.size() > 0) {
                    return intent;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting superuser app", e);
        }
        return null;
    }

    public static CharSequence getAppName(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        CharSequence name = null;
        try {
            name = pm.getApplicationLabel(intent.resolveActivityInfo(pm, 0).applicationInfo);
        } catch (Exception e) {
            Log.w(TAG, "Error fetching application name for intent " + intent, e);
        }
        return name;
    }

    public static String readFileToString(File file) throws IOException {
        InputStream in = null;
        byte[] bytes = null;
        try {
            in = new FileInputStream(file);
            bytes = new byte[(int) file.length()];
            int len = bytes.length;
            int total = 0;

            while (total < len) {
                int result = in.read(bytes, total, len - total);
                if (result == -1) {
                    break;
                }
                total += result;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return new String(bytes);
    }

    public static void writeStringToFile(File file, String string) throws IOException {
        BufferedOutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            os.write(string.getBytes());
            os.flush();
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    public static int waitForProcess(String command, long timeout) {
        long timeoutNs = timeout * 1000000l;
        long startTime = System.nanoTime();
        int pid = -1;
        while ((System.nanoTime() - startTime) < timeoutNs && pid < 0) {
            pid = Utils.findProcessByCommand(command);
        }
        return pid;
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void logStackTrace(String tag, String message) {
        try {
            throw new RuntimeException();
        } catch (RuntimeException e) {
            Log.v(TAG, message, e);
        }
    }

    public static boolean isPackageInstalled(String packageName, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("ResourceType")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isMediaProjectionPermanent(Context context) {
        try {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            Method method = appOpsManager.getClass().getMethod("noteOpNoThrow", Integer.TYPE, Integer.TYPE, String.class);
            Integer mode = (Integer) method.invoke(appOpsManager, 46, android.os.Process.myUid(), context.getPackageName());
            Log.v(TAG, "isMediaProjectionPermanent: " + mode);
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Log.w(TAG, "Can't obtain MediaProjection settings", e);
            return false;
        }
    }
}
