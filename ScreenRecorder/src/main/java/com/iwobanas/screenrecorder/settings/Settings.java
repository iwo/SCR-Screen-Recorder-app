package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.iwobanas.screenrecorder.ResolutionsManager;

public class Settings {
    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";
    private static final String AUDIO_SOURCE = "AUDIO_SOURCE";
    private static final String RESOLUTION_WIDTH = "RESOLUTION_WIDTH";
    private static final String RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT";
    private static final String FRAME_RATE = "FRAME_RATE";

    private static Settings instance;

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }
    }

    public static synchronized Settings getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Settings not initialized");
        }
        return instance;
    }

    private SharedPreferences preferences;

    private AudioSource audioSource = AudioSource.MIC;

    private Resolution resolution;

    private ResolutionsManager resolutionsManager;

    private int frameRate = 15;

    private Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        resolutionsManager = new ResolutionsManager(context);
        readPreferences();
    }

    private void readPreferences() {
        String audioSource = preferences.getString(AUDIO_SOURCE, AudioSource.MIC.name());
        this.audioSource = AudioSource.valueOf(audioSource);

        int resolutionWidth = preferences.getInt(RESOLUTION_WIDTH, -1);
        if (resolutionWidth != -1) {
            int resolutionHeight = preferences.getInt(RESOLUTION_HEIGHT, 0);
            resolution = resolutionsManager.getResolution(resolutionWidth, resolutionHeight);
        }

        frameRate = preferences.getInt(FRAME_RATE, 15);
    }

    public AudioSource getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AUDIO_SOURCE, audioSource.name());
        editor.commit();
    }

    public Resolution getResolution() {
        if (resolution == null)
            return resolutionsManager.getDefaultResolution();

        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
        if (resolution != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(RESOLUTION_WIDTH, resolution.getWidth());
            editor.putInt(RESOLUTION_HEIGHT, resolution.getHeight());
            editor.commit();
        }
    }

    public Resolution[] getResolutions() {
        return resolutionsManager.getResolutions();
    }

    public Resolution getDefaultResolution() {
        return resolutionsManager.getDefaultResolution();
    }

    public void setFrameRate(int value) {
        frameRate = value;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(FRAME_RATE, frameRate);
        editor.commit();
    }

    public int getFrameRate() {
        return frameRate;
    }

}


