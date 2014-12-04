package com.iwobanas.screenrecorder;

import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NativeCommands implements INativeCommands {

    private static final String TAG = "scr_NativeCommands";
    private static INativeCommands instance;

    private INativeCommandRunner runner;
    private volatile CountDownLatch resultCountdownLatch;
    private volatile int lastCommandResult;

    private NativeCommands() {
    }

    @Override
    public String getSuVersion() {
        return runner.getSuVersion();
    }

    @Override
    public int killSignal(int pid) {
        return runAsyncCommand("kill_kill " + pid, 2);
    }

    @Override
    public int termSignal(int pid) {
        return runAsyncCommand("kill_term " + pid, 2);
    }

    @Override
    public int mountAudioMaster(String path) {
        return runAsyncCommand("mount_audio_master " + path, 5);
    }

    @Override
    public int mountAudio(String path) {
        return runAsyncCommand("mount_audio " + path, 5);
    }

    @Override
    public int unmountAudio() {
        return runAsyncCommand("unmount_audio", 5);
    }

    @Override
    public int unmountAudioMaster() {
        return runAsyncCommand("unmount_audio_master", 5);
    }

    @Override
    public int logcat(String path) {
        return runAsyncCommand("logcat " + path, 30);
    }

    //TODO: implement proper commands queue
    private int runAsyncCommand(String command, long timeout) {
        resultCountdownLatch = new CountDownLatch(1);
        runner.runCommand(command);
        try {
            if (resultCountdownLatch.await(timeout, TimeUnit.SECONDS)) {
                return lastCommandResult;
            } else {
                return -10;
            }
        } catch (InterruptedException e) {
            return -20;
        }
    }

    @Override
    public void setCommandRunner(INativeCommandRunner runner) {
        this.runner = runner;
    }

    //TODO: ensure that results are correctly matched against commands
    @Override
    public void notifyCommandResult(String command, int result) {
        lastCommandResult = result;
        if (resultCountdownLatch != null) {
            resultCountdownLatch.countDown();
        } else {
            Log.e(TAG, "Unexpected result! " + command);
        }
    }

    public static synchronized INativeCommands getInstance() {
        if (instance == null) {
            instance = new NativeCommands();
        }
        return instance;
    }
}
