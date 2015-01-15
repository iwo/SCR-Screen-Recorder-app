package com.iwobanas.screenrecorder;

import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class NativeCommands implements INativeCommands {

    private static final String TAG = "scr_NativeCommands";
    private static INativeCommands instance;

    private INativeCommandRunner runner;
    private AtomicInteger nextRequestId = new AtomicInteger(1);
    private volatile CountDownLatch resultCountdownLatch;
    private volatile int lastCommandRequestId;
    private volatile int lastCommandResult;

    private NativeCommands() {
    }

    @Override
    public String getSuVersion() {
        return runner == null ? null : runner.getSuVersion();
    }

    @Override
    public boolean isExecBlocked() {
        return runner != null && runner.isExecBlocked();
    }

    @Override
    public int killSignal(int pid) {
        return runAsyncCommand("kill_kill", String.valueOf(pid), 2);
    }

    @Override
    public int termSignal(int pid) {
        return runAsyncCommand("kill_term", String.valueOf(pid), 2);
    }

    @Override
    public int mountAudioMaster(String path) {
        if (runner != null && runner.isExecBlocked()) {
            return runShellCommand("su", "--mount-master",  "-c", runner.getExecutable(), "mount_audio", path);
        }
        return runAsyncCommand("mount_audio_master", path, 5);
    }

    @Override
    public int mountAudio(String path) {
        return runAsyncCommand("mount_audio", path, 5);
    }

    @Override
    public int unmountAudio() {
        return runAsyncCommand("unmount_audio", "", 5);
    }

    @Override
    public int unmountAudioMaster() {
        if (runner != null && runner.isExecBlocked()) {
            return runShellCommand("su", "--mount-master",  "-c", runner.getExecutable(), "unmount_audio");
        }
        return runAsyncCommand("unmount_audio_master", "", 5);
    }

    @Override
    public int installAudio(String path) {
        return runAsyncCommand("install_audio", path, 20);
    }

    @Override
    public int uninstallAudio() {
        return runAsyncCommand("uninstall_audio", "", 5);
    }

    @Override
    public int logcat(String path) {
        if (runner != null && runner.isExecBlocked()) {
            return -50; // ReportBugTask will run it's own ShellCommand
        }
        return runAsyncCommand("logcat", path, 30);
    }

    //TODO: implement proper commands queue
    private int runAsyncCommand(String command, String args, long timeout) {
        if (runner == null) {
            return -30;
        }

        resultCountdownLatch = new CountDownLatch(1);
        lastCommandRequestId = nextRequestId.getAndIncrement();
        if (!runner.runCommand(command, lastCommandRequestId, args)) {
            return -40;
        }
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

    private int runShellCommand(String... command) {
        ShellCommand shellCommand = new ShellCommand(command);
        shellCommand.setTimeoutMillis(30000);
        shellCommand.execute();
        return shellCommand.exitValue();
    }

    @Override
    public void setCommandRunner(INativeCommandRunner runner) {
        this.runner = runner;
    }

    @Override
    public void notifyCommandResult(int requestId, int result) {
        if (requestId >= 0 && requestId != lastCommandRequestId) {
            Log.w(TAG, "Ignoring stale command result " + requestId + " " + result);
            return;
        }
        lastCommandResult = result;
        if (resultCountdownLatch != null) {
            resultCountdownLatch.countDown();
        } else {
            Log.e(TAG, "Unexpected result! " + requestId);
        }
    }

    public static synchronized INativeCommands getInstance() {
        if (instance == null) {
            instance = new NativeCommands();
        }
        return instance;
    }
}
