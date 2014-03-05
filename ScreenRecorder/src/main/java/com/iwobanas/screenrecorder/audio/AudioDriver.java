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

    public AudioDriver(Context context) {
        this.context = context;
        new CheckInstallationAsyncTask(context, this).execute();
    }

    public boolean shouldInstall() {
        return (Settings.getInstance().getAudioSource() == AudioSource.INTERNAL
                && (status == InstallationStatus.NOT_INSTALLED
                || status == InstallationStatus.CHECKING
                || status == InstallationStatus.UNSPECIFIED
                || status == InstallationStatus.OUTDATED)
        );
    }

    public boolean isReady() {
        return !shouldInstall()
                || status == InstallationStatus.INSTALLED
                || status == InstallationStatus.INSTALLATION_FAILURE;
    }

    public void install() {
        if (status != InstallationStatus.NOT_INSTALLED && status != InstallationStatus.INSTALLATION_FAILURE) {
            Log.e(TAG, "Attempting to install in incorrect state: " + status);
        }
        setInstallationStatus(InstallationStatus.INSTALLING);
        new InstallAsyncTask(context, this).execute();
    }

    public void uninstallIfNeeded() {
        if (status == InstallationStatus.INSTALLED || status == InstallationStatus.OUTDATED) {
            uninstall();
        }
    }

    public void uninstall() {
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
        this.status = status;
        for (OnInstallListener listener : listeners) {
            listener.onInstall(AudioDriver.this.status);
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
