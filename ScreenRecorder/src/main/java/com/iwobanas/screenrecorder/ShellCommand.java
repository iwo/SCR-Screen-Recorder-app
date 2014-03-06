package com.iwobanas.screenrecorder;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;

public class ShellCommand {
    private static final String TAG = "scr_cmd";

    private String[] command;
    private Process process;
    private String input;
    private StringBuilder outputStringBuilder = new StringBuilder();
    private StringBuilder errorStringBuilder = new StringBuilder();
    private boolean executionCompleted = false;
    private String errorLogTag;
    private String outLogTag;

    public ShellCommand(String[] command) {
        this.command = command;
    }

    public void execute() {
        try {
            Log.v(TAG, "Executing command: " + Arrays.toString(command));
            process = Runtime.getRuntime().exec(command);
            if (process == null) {
                Log.e(TAG, "Process not created: " + Arrays.toString(command));
                return;
            }
            Log.v(TAG, "Process created");
            new Thread(new ProcessStreamReader(process.getInputStream(), outputStringBuilder, outLogTag)).start();
            new Thread(new ProcessStreamReader(process.getErrorStream(), errorStringBuilder, errorLogTag)).start();
            passInput();
            Log.v(TAG, "Waiting for process to exit");
            process.waitFor();
            executionCompleted = true;
            Log.v(TAG, "Process completed with exit value: " + process.exitValue());
        } catch (IOException e) {
            Log.e(TAG, "Exception in " + Arrays.toString(command), e);
        } catch (InterruptedException e) {
            Log.e(TAG, "Exception in " + Arrays.toString(command), e);
        }
    }

    private void passInput() {
        if (process != null && input != null) {
            try {
                OutputStream inputStream = process.getOutputStream();
                inputStream.write(input.getBytes());
                inputStream.flush();
                Log.v(TAG, "Input passed");
            } catch (IOException e) {
                Log.w(TAG, "Error passing input: " + e.getMessage());
            }
        }
    }

    public boolean isExecutionCompleted() {
        return executionCompleted;
    }

    public int exitValue() {
        if (process == null) {
            return -1;
        }
        return process.exitValue();
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setOutLogTag(String outLogTag) {
        this.outLogTag = outLogTag;
    }

    public void setErrorLogTag(String errorLogTag) {
        this.errorLogTag = errorLogTag;
    }

    public String getOutput() {
        return outputStringBuilder.toString();
    }

    class ProcessStreamReader implements Runnable {
        private BufferedReader reader;
        private StringBuilder stringBuilder;
        private String logTag;

        public ProcessStreamReader(InputStream stream, StringBuilder stringBuilder, String logTag) {
            reader = new BufferedReader(new InputStreamReader(stream));
            this.stringBuilder = stringBuilder;
            this.logTag = logTag;
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                    if (logTag != null) {
                        Log.v(logTag, line);
                    }
                }
            } catch (IOException ignored) {}
        }
    }
}
