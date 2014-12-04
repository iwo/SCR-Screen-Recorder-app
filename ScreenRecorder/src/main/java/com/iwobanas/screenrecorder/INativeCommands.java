package com.iwobanas.screenrecorder;

public interface INativeCommands {
    String getSuVersion();
    int killSignal(int pid);
    int termSignal(int pid);
    int mountAudioMaster(String path);
    int mountAudio(String path);
    int unmountAudio();
    int unmountAudioMaster();
    int logcat(String path);
    void setCommandRunner(INativeCommandRunner runner);
    void notifyCommandResult(String command, int result);
}
