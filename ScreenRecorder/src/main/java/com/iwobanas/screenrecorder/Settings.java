package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";
    private static final String AUDIO_SOURCE = "AUDIO_SOURCE";

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

    private Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        readPreferences();
    }

    private void readPreferences() {
        String audioSource = preferences.getString(AUDIO_SOURCE, AudioSource.MIC.name());
        this.audioSource = AudioSource.valueOf(audioSource);
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
}


