package com.iwobanas.screenrecorder.settings;

public enum AudioSource {
    MIC("m", false),
    INTERNAL("i", true),
    MIX("+", true),
    MUTE("x", false);

    private String command;
    private boolean requiresDriver;

    AudioSource(String command, boolean requiresDriver) {
        this.command = command;
        this.requiresDriver = requiresDriver;
    }

    public String getCommand() {
        return command;
    }

    public boolean getRequiresDriver() {
        return requiresDriver;
    }
}
