package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.Handler;

import com.iwobanas.screenrecorder.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class AudioDriver {
    private Context context;
    private Set<OnInstallListener> listeners = new HashSet<OnInstallListener>();
    private Handler handler;
    private InstallationStatus status = null;
    private int driverVersion = 0;
    private int samplingRate = 0;

    public AudioDriver(Context context) {
        this.context = context;
        handler = new Handler();
        driverVersion = context.getResources().getInteger(R.integer.audio_driver_version);
    }

    public void install() {
        setInstallationStatus(InstallationStatus.INSTALLING);
        new InstallerThread(context, this, true, driverVersion).start();
    }

    public void uninstall() {
        setInstallationStatus(InstallationStatus.UNINSTALLING);
        new InstallerThread(context, this, false).start();
    }

    public int getSamplingRate() {
        if (samplingRate == 0) {
            samplingRate = AudioPolicyUtils.getMaxPrimarySamplingRate("/etc/audio_policy.conf");
            if (samplingRate <= 0) {
                samplingRate = 44100;
            }
        }
        return samplingRate;
    }

    public InstallationStatus getInstallationStatus() {
        if (status == null) {
            initializeStatus();
        }
        return status;
    }

    protected void setInstallationStatus(InstallationStatus status) {
        this.status = status;
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (OnInstallListener listener : listeners) {
                    listener.onInstall(AudioDriver.this.status);
                }
            }
        });
    }

    private void initializeStatus() {
        try {
            File versionFile = new File("/system/lib/hw/scr_module_version");
            int version = 0;
            if (versionFile.exists()) {
                Scanner scanner = new Scanner(versionFile);
                if (scanner.hasNextInt()) {
                    version = scanner.nextInt();
                }
                if (version == driverVersion) {
                    status = InstallationStatus.INSTALLED;
                } else {
                    status = InstallationStatus.OUTDATED;
                }
            }
        } catch (FileNotFoundException ignored) {
        }
        if (status == null) {
            status = InstallationStatus.NOT_INSTALLED;
        }
    }

    public void addInstallListener(OnInstallListener listener) {
        listeners.add(listener);
    }

    public void removeInstallListener(OnInstallListener listener) {
        listeners.remove(listener);
    }

    public enum InstallationStatus {
        NOT_INSTALLED,
        INSTALLING,
        INSTALLED,
        UNINSTALLING,
        INSTALLATION_FAILURE,
        OUTDATED
    }

    public interface OnInstallListener {
        void onInstall(InstallationStatus status);
    }
}
