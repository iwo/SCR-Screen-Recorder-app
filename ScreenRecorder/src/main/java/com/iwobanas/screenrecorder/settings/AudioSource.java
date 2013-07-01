package com.iwobanas.screenrecorder.settings;

public enum AudioSource {
    MIC("m"),
    MUTE("x");

    private String command;

    AudioSource(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
