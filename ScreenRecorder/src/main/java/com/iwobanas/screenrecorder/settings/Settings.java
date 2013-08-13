package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.iwobanas.screenrecorder.ResolutionsManager;
import com.iwobanas.screenrecorder.Utils;

public class Settings {
    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";
    private static final String AUDIO_SOURCE = "AUDIO_SOURCE";
    private static final String RESOLUTION_WIDTH = "RESOLUTION_WIDTH";
    private static final String RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT";
    private static final String FRAME_RATE = "FRAME_RATE";
    private static final String TRANSFORMATION = "TRANSFORMATION";
    private static final String COLOR_FIX = "COLOR_FIX";
    private static final String HIDE_ICON = "HIDE_ICON";
    private static final String DEFAULT_RESOLUTION_WIDTH = "DEFAULT_RESOLUTION_WIDTH";
    private static final String DEFAULT_RESOLUTION_HEIGHT = "DEFAULT_RESOLUTION_HEIGHT";
    private static final String DEFAULT_TRANSFORMATION = "DEFAULT_TRANSFORMATION";
    private static final String DEFAULT_COLOR_FIX = "DEFAULT_COLOR_FIX";
    private static final String DEFAULTS_UPDATE_TIMESTAMP = "DEFAULTS_UPDATE_TIMESTAMP";

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

    private Resolution defaultResolution;

    private ResolutionsManager resolutionsManager;

    private int frameRate = 15;

    private Transformation transformation = Transformation.GPU;

    private Transformation defaultTransformation = Transformation.GPU;

    private boolean colorFix = false;

    private boolean defaultColorFix = false;

    private boolean hideIcon = false;

    private Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        resolutionsManager = new ResolutionsManager(context);
        readPreferences();
        if (shouldUpdateDefaults()) {
            new LoadDefaultsAsyncTask(Utils.getAppVersion(context)).execute();
        }
    }

    private void readPreferences() {
        String audioSource = preferences.getString(AUDIO_SOURCE, AudioSource.MIC.name());
        this.audioSource = AudioSource.valueOf(audioSource);

        int defaultResolutionWidth = preferences.getInt(DEFAULT_RESOLUTION_WIDTH, -1);
        if (defaultResolutionWidth != -1) {
            int defaultResolutionHeight = preferences.getInt(DEFAULT_RESOLUTION_HEIGHT, 0);
            defaultResolution = resolutionsManager.getResolution(defaultResolutionWidth, defaultResolutionHeight);
        }

        int resolutionWidth = preferences.getInt(RESOLUTION_WIDTH, -1);
        if (resolutionWidth != -1) {
            int resolutionHeight = preferences.getInt(RESOLUTION_HEIGHT, 0);
            resolution = resolutionsManager.getResolution(resolutionWidth, resolutionHeight);
        }

        frameRate = preferences.getInt(FRAME_RATE, 15);

        String defaultTransformation = preferences.getString(DEFAULT_TRANSFORMATION, Transformation.GPU.name());
        this.defaultTransformation = Transformation.valueOf(defaultTransformation);

        String transformation = preferences.getString(TRANSFORMATION, defaultTransformation);
        this.transformation = Transformation.valueOf(transformation);

        defaultColorFix = preferences.getBoolean(DEFAULT_COLOR_FIX, false);
        colorFix = preferences.getBoolean(COLOR_FIX, defaultColorFix);

        hideIcon = preferences.getBoolean(HIDE_ICON, false);
    }

    public void updateDefaults(String resolutionWidth, String resolutionHeight, String transformation, String colorFix) {
        if (resolutionWidth != null && resolutionWidth.length() > 0 &&
            resolutionHeight != null && resolutionHeight.length() > 0) {
            int w = Integer.parseInt(resolutionWidth);
            int h = Integer.parseInt(resolutionHeight);
            defaultResolution = resolutionsManager.getResolution(w, h);
        }
        if (transformation != null && transformation.length() > 0) {
            defaultTransformation = Transformation.valueOf(transformation);
        }
        if (colorFix != null && colorFix.length() > 0) {
            defaultColorFix = Boolean.valueOf(colorFix);
        }

        saveDefaults();
        readPreferences(); // refresh preferences to restore update defaults
    }

    private void saveDefaults() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(DEFAULTS_UPDATE_TIMESTAMP, System.currentTimeMillis());
        if (defaultResolution != null) {
            editor.putInt(DEFAULT_RESOLUTION_WIDTH, defaultResolution.getWidth());
            editor.putInt(DEFAULT_RESOLUTION_HEIGHT, defaultResolution.getHeight());
        } else {
            editor.remove(DEFAULT_RESOLUTION_WIDTH);
            editor.remove(DEFAULT_RESOLUTION_HEIGHT);
        }

        if (defaultTransformation != null) {
            editor.putString(DEFAULT_TRANSFORMATION, defaultTransformation.name());
        } else {
            editor.remove(DEFAULT_TRANSFORMATION);
        }

        editor.putBoolean(DEFAULT_COLOR_FIX, defaultColorFix);

        editor.commit();
    }

    private Boolean shouldUpdateDefaults() {
        long time = System.currentTimeMillis() - preferences.getLong(DEFAULTS_UPDATE_TIMESTAMP, 0);
        return (time > 24 * 60 * 60 * 1000);
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
            return getDefaultResolution();

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
        if (defaultResolution != null) {
            return defaultResolution;
        }
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
        editor.putBoolean(HIDE_ICON, hideIcon);
        editor.commit();
    }

    public void restoreDefault() {
        setAudioSource(AudioSource.MIC);
        setResolution(getDefaultResolution());
        setFrameRate(15);
        setTransformation(defaultTransformation);
        setColorFix(defaultColorFix);
        setHideIcon(false);
    }
}


