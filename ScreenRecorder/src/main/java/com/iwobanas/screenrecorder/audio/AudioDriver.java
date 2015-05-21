package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.RecordingInfo;
import com.iwobanas.screenrecorder.settings.Settings;
import com.iwobanas.screenrecorder.stats.AudioModuleStatsAsyncTask;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

public class AudioDriver {
    private static final String TAG = "scr_AudioDriver";
    private static final String CONFIG_FILE = "/system/lib/hw/scr_audio.conf";

    private Context context;
    private Set<OnInstallListener> listeners = new HashSet<OnInstallListener>();
    private InstallationStatus status = InstallationStatus.NEW;
    private int samplingRate = 0;
    private boolean installScheduled = false;
    private boolean uninstallScheduled = false;
    private Long installId;
    private StabilityMonitorAsyncTask stabilityMonitor;
    private boolean requiresHardInstall = (Build.VERSION.SDK_INT == 17);
    private boolean retryHardInstall;

    public AudioDriver(Context context) {
        this.context = context;
    }

    public void check() {
        if (status == InstallationStatus.NEW) {
            setInstallationStatus(InstallationStatus.CHECKING);
            new CheckInstallationAsyncTask(context, this).execute();
        }
    }

    public boolean shouldInstall() {
        return (Settings.getInstance().getAudioSource().getRequiresDriver()
                && (status == InstallationStatus.NOT_INSTALLED
                || status == InstallationStatus.NEW
                || status == InstallationStatus.CHECKING
                || status == InstallationStatus.UNINSTALLING
                || status == InstallationStatus.UNSPECIFIED
                || status == InstallationStatus.OUTDATED)
        );
    }

    public void install() {
        if (status == InstallationStatus.NEW || status == InstallationStatus.CHECKING || status == InstallationStatus.UNINSTALLING) {
            Log.w(TAG, "Attempting to install when " + status + ". Scheduling installation.");
            installScheduled = true;
            uninstallScheduled = false;
            return;
        }
        if (status != InstallationStatus.NOT_INSTALLED
                && status != InstallationStatus.INSTALLATION_FAILURE
                && status != InstallationStatus.UNSPECIFIED
                && status != InstallationStatus.OUTDATED) {
            Log.e(TAG, "Attempting to install in incorrect state: " + status);
            return;
        }
        installId = System.currentTimeMillis();
        setInstallationStatus(InstallationStatus.INSTALLING);
        new InstallAsyncTask(context, this, installId).execute();
    }

    public boolean shouldUninstall() {
        return status == InstallationStatus.CHECKING
                || status == InstallationStatus.INSTALLING
                ||status == InstallationStatus.INSTALLED
                || status == InstallationStatus.INSTALLATION_FAILURE
                || status == InstallationStatus.UNSTABLE
                || status == InstallationStatus.UNSPECIFIED;
    }

    public void uninstall() {
        if (status == InstallationStatus.NEW || status == InstallationStatus.CHECKING || status == InstallationStatus.INSTALLING) {
            Log.w(TAG, "Attempting to uninstall when " + status + ". Scheduling uninstallation.");
            installScheduled = false;
            uninstallScheduled = true;
            return;
        }
        if (status != InstallationStatus.INSTALLED
                && status != InstallationStatus.OUTDATED
                && status != InstallationStatus.INSTALLATION_FAILURE
                && status != InstallationStatus.UNSTABLE
                && status != InstallationStatus.UNSPECIFIED) {
            Log.e(TAG, "Attempting to uninstall in incorrect state: " + status);
            return;
        }
        if (stabilityMonitor != null) {
            stabilityMonitor.cancel(true);
        }
        installId = System.currentTimeMillis();
        setInstallationStatus(InstallationStatus.UNINSTALLING);
        new UninstallAsyncTask(context, this, installId).execute();
    }

    public int getSamplingRate() {
        if (samplingRate == 0) {
            samplingRate = AudioPolicyUtils.getMaxPrimarySamplingRate();
            if (samplingRate <= 0) {
                //TODO: on ICS determine if 44.1kHz or 48kHz is used
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
            if (status == InstallationStatus.NOT_INSTALLED) {
                Log.v(TAG, "Already uninstalled. Scheduled uninstall cancelled.");
            } else {
                Log.v(TAG, "Starting scheduled uninstall");
                uninstall();
            }
        } else {
            if (status == InstallationStatus.INSTALLED) {
                stabilityMonitor = new StabilityMonitorAsyncTask(context, this, installId);
                stabilityMonitor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            for (OnInstallListener listener : listeners) {
                listener.onInstall(AudioDriver.this.status);
            }
        }
    }

    public void startRecording() {
        if (stabilityMonitor != null) {
            stabilityMonitor.cancel(true);
        }
    }

    public void logStats(RecordingInfo recordingInfo) {
        new AudioModuleStatsAsyncTask(context, recordingInfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void addInstallListener(OnInstallListener listener) {
        listeners.add(listener);
    }

    public void removeInstallListener(OnInstallListener listener) {
        listeners.remove(listener);
    }

    public void updateConfig(boolean enableMix, int micGain) {
        try {
            String mixMic = enableMix ? "1" : "0";
            File configFile = new File(CONFIG_FILE);
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write(String.valueOf(getVolumeGain()) + " " + mixMic + " " + micGain + "\n");
            fileWriter.close();
            if (!configFile.setReadable(true, false)) {
                Log.w(TAG, "Error setting read permission on " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Error setting audio gain", e);
        }
    }

    private int getVolumeGain() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int gain = 1;
        double volume = (double) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / (1.0 + audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        if (volume < 1.0 && volume > 0.001) {
            gain = (int) (1.0/(volume * volume));
        }
        gain = Math.min(gain, 16);
        Log.v(TAG, "Music volume " + volume + " setting gain to " + gain);
        return gain;
    }

    public void setRequiresHardInstall() {
        this.requiresHardInstall = true;
    }

    public boolean getRequiresHardInstall() {
        return requiresHardInstall;
    }

    public boolean getRetryHardInstall() {
        return retryHardInstall;
    }

    public void setRetryHardInstall(boolean retryHardInstall) {
        this.retryHardInstall = retryHardInstall;
    }

    public interface OnInstallListener {
        void onInstall(InstallationStatus status);
    }
}
