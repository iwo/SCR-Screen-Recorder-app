package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.Handler;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class AudioDriver {
    private Context context;
    private Set<OnInstallListener> listeners = new HashSet<OnInstallListener>();
    private Handler handler;
    private InstallationStatus status = InstallationStatus.NOT_INSTALLED;

    public AudioDriver(Context context) {
        this.context = context;
        handler = new Handler();
    }

    public void install() {
        setInstallationStatus(InstallationStatus.INSTALLING);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setInstallationStatus(InstallationStatus.INSTALLED);
            }
        }, 2000);
    }

    public void uninstall() {
        setInstallationStatus(InstallationStatus.UNINSTALLING);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                setInstallationStatus(InstallationStatus.NOT_INSTALLED);
            }
        }, 2000);
    }

    public InstallationStatus getInstallationStatus() {
        return status;
    }

    private void setInstallationStatus(InstallationStatus status) {
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
        INSTALLATION_FAILURE
    }

    public interface OnInstallListener {
        void onInstall(InstallationStatus status);
    }
}
