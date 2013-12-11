package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class InstallerThread extends Thread {
    private static final String TAG = "scr_InstallerThread";
    private Context context;
    private AudioDriver audioDriver;
    private Process process;
    private OutputStream stdin;
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

        File driverFile = new File(context.getFilesDir(), "audio.primary.default.so");
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
            return;
        }

        File policyFile = new File(context.getFilesDir(), "audio_policy.conf");
        try {
            Log.v(TAG, "Processing audio_policy.conf");
            AudioPolicyUtils.commentOutOutputs("/etc/audio_policy.conf", policyFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error processing policy file ", e);
            if (policyFile.exists()) {
                policyFile.delete();
            }
        }

        try {
            Log.d(TAG, "Starting installer process");
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", installer.getAbsolutePath()});
            Log.d(TAG, "Installer process started");
        } catch (IOException e) {
            error("Error starting installer process", e);
            return;
        }

        if (process == null) {
            error("Process not created");
            return;
        }

        String command = install ?
                "install_audio\n" + context.getFilesDir().getAbsolutePath() + "\n" + version + "\n" :
                "uninstall_audio\n";

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
            audioDriver.setInstallationStatus(install ?
                    AudioDriver.InstallationStatus.INSTALLED :
                    AudioDriver.InstallationStatus.NOT_INSTALLED);
        } else {
            error("Installation failed. exit value: " + exitValue);
        }
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
