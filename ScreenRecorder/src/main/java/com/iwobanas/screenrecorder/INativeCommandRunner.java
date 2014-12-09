package com.iwobanas.screenrecorder;

public interface INativeCommandRunner {
    String getSuVersion();
    void runCommand(String command, int requestId, String args);
}
