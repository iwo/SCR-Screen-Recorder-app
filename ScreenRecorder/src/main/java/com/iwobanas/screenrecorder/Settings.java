package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";
    private static final String AUDIO_SOURCE = "AUDIO_SOURCE";
    private static final String RESOLUTION_LABEL = "RESOLUTION_LABEL";
    private static final String RESOLUTION_WIDTH = "RESOLUTION_WIDTH";
    private static final String RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT";

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

    private Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        resolutionsManager = new ResolutionsManager(context);
        readPreferences();
    }

    private void readPreferences() {
        String audioSource = preferences.getString(AUDIO_SOURCE, AudioSource.MIC.name());
        this.audioSource = AudioSource.valueOf(audioSource);

        String resolutionLabel = preferences.getString(RESOLUTION_LABEL, null);
        if (resolutionLabel != null) {
            int width = preferences.getInt(RESOLUTION_WIDTH, 0);
            int height = preferences.getInt(RESOLUTION_HEIGHT, 0);
            resolution = new Resolution(resolutionLabel, width, height);
        }
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
            editor.putString(RESOLUTION_LABEL, resolution.getLabel());
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

    public static enum AudioSource {
        MIC("m"),
        MUTE("x");

        private String command;

        AudioSource(String command) {
            this.command = command;
        }

        String getCommand() {
            return command;
        }
    }

    public static class Resolution {
        private String label;
        private int width;
        private int height;

        public Resolution(String label, int width, int height) {
            this.label = label;
            this.width = width;
            this.height = height;
        }

        public String getLabel() {
            return label;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}


