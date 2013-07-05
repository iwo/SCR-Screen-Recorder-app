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
    private static final String TRANSFORMATION = "TRANSFORMATION";
    private static final String COLOR_FIX = "COLOR_FIX";
    private static final String HIDE_ICON = "HIDE_ICON";

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

    private Transformation transformation = Transformation.GPU;

    private boolean colorFix = false;

    private boolean hideIcon = false;

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

        String transformation = preferences.getString(TRANSFORMATION, Transformation.GPU.name());
        this.transformation = Transformation.valueOf(transformation);

        colorFix = preferences.getBoolean(COLOR_FIX, false);
        hideIcon = preferences.getBoolean(HIDE_ICON, false);
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

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TRANSFORMATION, transformation.name());
        editor.commit();
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public void setColorFix(boolean colorFix) {
        this.colorFix = colorFix;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(COLOR_FIX, colorFix);
        editor.commit();
    }

    public boolean getColorFix() {
        return colorFix;
    }

    public boolean getHideIcon() {
        return hideIcon;
    }

    public void setHideIcon(boolean hideIcon) {
        this.hideIcon = hideIcon;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(HIDE_ICON, colorFix);
        editor.commit();
    }

    public void restoreDefault() {
        setAudioSource(AudioSource.MIC);
        setResolution(getDefaultResolution());
        setFrameRate(15);
        setTransformation(Transformation.GPU);
        setColorFix(false);
        setHideIcon(false);
    }
}


