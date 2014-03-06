package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.util.Log;

import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Settings;

import java.util.HashSet;
import java.util.Set;

public class AudioDriver {
    private static final String TAG = "scr_AudioDriver";
    private Context context;
    private Set<OnInstallListener> listeners = new HashSet<OnInstallListener>();
    private InstallationStatus status = InstallationStatus.CHECKING;
    private int samplingRate = 0;
    private boolean installScheduled = false;
    private boolean uninstallScheduled = false;

    public AudioDriver(Context context) {
        this.context = context;
        new CheckInstallationAsyncTask(context, this).execute();
    }

    public boolean shouldInstall() {
        return (Settings.getInstance().getAudioSource() == AudioSource.INTERNAL
                && (status == InstallationStatus.NOT_INSTALLED
                || status == InstallationStatus.CHECKING
                || status == InstallationStatus.UNINSTALLING
                || status == InstallationStatus.UNSPECIFIED
                || status == InstallationStatus.OUTDATED)
        );
    }

    public boolean isReady() {
        return !shouldInstall() && status != InstallationStatus.UNINSTALLING && status != InstallationStatus.INSTALLING;
    }

    public void install() {
        if (status == InstallationStatus.CHECKING || status == InstallationStatus.UNINSTALLING) {
            Log.w(TAG, "Attempting to install when " + status + ". Scheduling installation.");
            installScheduled = true;
            uninstallScheduled = false;
            return;
        }
        if (status != InstallationStatus.NOT_INSTALLED && status != InstallationStatus.INSTALLATION_FAILURE) {
            Log.e(TAG, "Attempting to install in incorrect state: " + status);
            return;
        }
        setInstallationStatus(InstallationStatus.INSTALLING);
        new InstallAsyncTask(context, this).execute();
    }

    public boolean shouldUninstall() {
        return status == InstallationStatus.CHECKING
                || status == InstallationStatus.INSTALLING
                ||status == InstallationStatus.INSTALLED
                || status == InstallationStatus.INSTALLATION_FAILURE
                || status == InstallationStatus.UNSPECIFIED;
    }

    public void uninstall() {
        if (status == InstallationStatus.CHECKING || status == InstallationStatus.INSTALLING) {
            Log.w(TAG, "Attempting to uninstall when " + status + ". Scheduling uninstallation.");
            installScheduled = false;
            uninstallScheduled = true;
            return;
        }
        if (status != InstallationStatus.INSTALLED && status != InstallationStatus.OUTDATED) {
            Log.e(TAG, "Attempting to uninstall in incorrect state: " + status);
        }
        setInstallationStatus(InstallationStatus.UNINSTALLING);
        new UninstallAsyncTask(context, this).execute();
    }

    public int getSamplingRate() {
        if (samplingRate == 0) {
            samplingRate = AudioPolicyUtils.getMaxPrimarySamplingRate();
            if (samplingRate <= 0) {
                samplingRate = 44100;
            }
        }
        return samplingRate;
    }

    public InstallationStatus getInstallationStatus() {
        return status;
    }

    protected void setInstallationStatus(InstallationStatus status) {
        Log.v(TAG, status != null ? status.name() : "null");
        this.status = status;
        if (installScheduled) {
            installScheduled = false;
            Log.v(TAG, "Starting scheduled install");
            install();
        } else if (uninstallScheduled) {
            uninstallScheduled = false;
            Log.v(TAG, "Starting scheduled uninstall");
            uninstall();
        } else {
            for (OnInstallListener listener : listeners) {
                listener.onInstall(AudioDriver.this.status);
            }
        }
    }

    public void addInstallListener(OnInstallListener listener) {
        listeners.add(listener);
    }

    public void removeInstallListener(OnInstallListener listener) {
        listeners.remove(listener);
    }

    public interface OnInstallListener {
        void onInstall(InstallationStatus status);
    }
}
