package com.iwobanas.screenrecorder;

public interface INativeCommands {
    String getSuVersion();
    boolean isExecBlocked();
    int killSignal(int pid);
    int termSignal(int pid);
    int mountAudioMaster(String path);
    int mountAudio(String path);
    int unmountAudio();
    int unmountAudioMaster();
    int installAudio(String path);
    int uninstallAudio();
    int logcat(String path);
    void setCommandRunner(INativeCommandRunner runner);
    void notifyCommandResult(int requestId, int result);
}
