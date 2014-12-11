package com.iwobanas.screenrecorder;

public interface INativeCommandRunner {
    String getSuVersion();
    boolean runCommand(String command, int requestId, String args);
}
