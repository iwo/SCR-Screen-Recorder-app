package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

public class InstallerThread extends Thread {
    private static final String TAG = "scr_InstallerThread";
    private Context context;
    private AudioDriver audioDriver;
    private Process process;
    private OutputStream stdin;
    private InputStream stdout;
    private boolean install;
    private int version;

    public InstallerThread(Context context, AudioDriver audioDriver, boolean install) {
        this(context, audioDriver, install, 0);
    }

    public InstallerThread(Context context, AudioDriver audioDriver, boolean install, int version) {
        this.context = context;
        this.audioDriver = audioDriver;
        this.install = install;
        this.version = version;
    }

    @Override
    public void run() {
        File installer = new File(context.getFilesDir(), "screenrec");
        if (!installer.exists()) {
            error("Installer file does not exit");
            return;
        }

        String command = null;

        if (install) {
            File hwDir = new File(context.getFilesDir(), "hw");
            if (!hwDir.exists() && !createHwDir(hwDir)) {
                Log.e(TAG, "Modules directory creation failed");
                return;
            }

            if (!extractDriver(hwDir)) {
                Log.e(TAG, "Extracting driver failed");
                return;
            }

            if (!createVersionFile(hwDir)) {
                Log.e(TAG, "Can't create version file");
                return;
            }

            cratePolicyFile();

            command = "mount_audio\n" + context.getFilesDir().getAbsolutePath() + "\n";
        } else {
            int oldVersion = getDriverVersion();
            if (oldVersion == 0) {
                Log.e(TAG, "Error reading driver version. Attempting uninstall anyway");
            }

            if (oldVersion < 3) {
                command = "uninstall_audio\n";
            } else {
                command = "unmount_audio\n";
            }
        }

        if (command == null) {
            Log.e(TAG, "No command specified!");
            return;
        }

        //TODO: make sure that --mount-master option is portable
        //TODO: sheck if mounts are shared etc
        try {
            Log.d(TAG, "Starting installer process");
            process = Runtime.getRuntime().exec(new String[]{"su", "--mount-master", "-c", installer.getAbsolutePath()});
            Log.d(TAG, "Installer process started");
        } catch (IOException e) {
            error("Error starting installer process", e);
            return;
        }

        if (process == null) {
            error("Process not created");
            return;
        }

        stdout = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
        try {
            String status = reader.readLine();
            if (!"ready".equals(status)) {
                try {
                    Log.d(TAG, "Starting installer process without additional su parameters");
                    process = Runtime.getRuntime().exec(new String[]{"su", "-c", installer.getAbsolutePath()});
                    Log.d(TAG, "Installer process started");
                } catch (IOException e) {
                    error("Error starting installer process", e);
                    return;
                }
            }
        } catch (IOException ignored) {}

        if (process == null) {
            error("Process not created");
            return;
        }

        stdin = process.getOutputStream();
        try {
            stdin.write(command.getBytes());
            stdin.flush();
        } catch (IOException e) {
            error("Error running command", e);
            return;
        }

        try {
            Log.d(TAG, "Waiting for installer process to exit");
            process.waitFor();
            Log.d(TAG, "Installer process finished");
        } catch (InterruptedException e) {
            error("Installer process interrupted", e);
        }

        int exitValue = process.exitValue();

        if (exitValue == 0) {
            int installedVersion = getDriverVersion();
            if (install) {
                if (installedVersion == version) {
                    audioDriver.setInstallationStatus(AudioDriver.InstallationStatus.INSTALLED);
                } else {
                    audioDriver.setInstallationStatus(AudioDriver.InstallationStatus.INSTALLATION_FAILURE);
                    Log.e(TAG, "Incorrect installed version (" + installedVersion + ") Something went wrong!");
                }
            } else {
                if (installedVersion == 0) {
                    audioDriver.setInstallationStatus(AudioDriver.InstallationStatus.NOT_INSTALLED);
                } else {
                    audioDriver.setInstallationStatus(AudioDriver.InstallationStatus.INSTALLATION_FAILURE);
                    Log.e(TAG, "Something went wrong with uninstallation. Version file still present");
                }
            }
        } else {
            error("Installation failed. exit value: " + exitValue);
        }
    }

    private boolean createVersionFile(File hwDir) {
        File versionFile = new File(hwDir, "scr_module_version");
        try {
            PrintWriter writer = new PrintWriter(versionFile);
            writer.write(String.valueOf(version));
            writer.close();
        } catch (FileNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean createHwDir(File hwDir) {
        if (!hwDir.mkdirs()) {
            Log.e(TAG, "Can't create hw dir");
            return false;
        }
        hwDir.setReadable(true, false);
        hwDir.setExecutable(true, false);
        File originalHwDir = new File("/system/lib/hw");

        for (String module : originalHwDir.list()) {
            File src = new File(originalHwDir, module);
            File dst = new File(hwDir, module);
            if (module.startsWith("audio.primary.")) {
                dst = new File(hwDir, "audio.original_primary." + module.substring(14));
            }
            if (!Utils.copyFile(src, dst)) {
                Log.e(TAG, "Can't copy file " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
                return false;
            }
            dst.setReadable(true, false);
        }
        return true;
    }

    private boolean extractDriver(File hwDir) {
        File driverFile = new File(hwDir, "audio.primary.default.so");
        try {
            Log.v(TAG, "Extracting driver binaries");
            if (Utils.isArm()) {
                Utils.extractResource(context, R.raw.audio, driverFile);
            } else if (Utils.isX86()) {
                Utils.extractResource(context, R.raw.audio_x86, driverFile);
            } else {
                error("Unsupported CPU: " + Build.CPU_ABI);
            }
        } catch (IOException e) {
            error("Error extracting driver", e);
            return false;
        }
        driverFile.setReadable(true, false);
        return true;
    }

    private void cratePolicyFile() {
        /*File policyFile = new File(context.getFilesDir(), "audio_policy.conf");
        try {
            Log.v(TAG, "Processing audio_policy.conf");
            AudioPolicyUtils.fixPolicyFile(policyFile.getAbsolutePath());
            policyFile.setReadable(true, false);
        } catch (IOException e) {
            Log.e(TAG, "Error processing policy file ", e);
            if (policyFile.exists()) {
                policyFile.delete();
            }
        }*/
    }

    private int getDriverVersion() {
        int version = 0;
        try {
            File versionFile = new File("/system/lib/hw/scr_module_version");

            if (versionFile.exists()) {
                Scanner scanner = new Scanner(versionFile);
                if (scanner.hasNextInt()) {
                    version = scanner.nextInt();
                }
            }
        } catch (FileNotFoundException ignored) {
        }
        return version;
    }

    private void error(String errorMessage) {
        error(errorMessage, null);
    }

    private void error(String errorMessage, Throwable tr) {
        if (tr != null) {
            Log.e(TAG, errorMessage, tr);
        } else {
            Log.e(TAG, errorMessage);
        }
        audioDriver.setInstallationStatus(AudioDriver.InstallationStatus.INSTALLATION_FAILURE);
    }
}
