package com.iwobanas.screenrecorder.stats;

import android.content.Context;
import android.util.Log;

import com.iwobanas.screenrecorder.RecordingInfo;
import com.iwobanas.screenrecorder.audio.AudioDriverInstaller;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AudioModuleStatsAsyncTask extends StatsBaseAsyncTask {
    private static final String TAG = "scr_AudioModuleStatsAT";
    private static final String BASE_URL = "http://www.iwobanas.com/scr/audio_module.php?";
    public static final int RETRY_TIME_MS = 3000;
    public static final int MAX_RECORD_AGE_S = 7;

    private RecordingInfo recordingInfo;
    private Context context;

    public AudioModuleStatsAsyncTask(Context context, RecordingInfo recordingInfo) {
        super(context);
        this.context = context;
        this.recordingInfo = recordingInfo;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        params.put("audio_source", Settings.getInstance().getAudioSource().name());
    }

    @Override
    protected Void doInBackground(Void... voids) {
        String[] logEntry = getRecentLogEntry();
        if (logEntry == null) {
            try {
                Thread.sleep(RETRY_TIME_MS);
            } catch (InterruptedException ignored) {}
        }
        logEntry = getRecentLogEntry();
        if (logEntry == null) {
            Log.w(TAG, "No audio log entry found");
            return null;
        }
        truncateLog();

        if (recordingInfo.file != null) {
            params.put("recording_id", recordingInfo.file.getName());
        }

        params.put("req_sample_rate", Settings.getInstance().getSamplingRate().getCommand());

        try {
            params.put("frames_read", validateLong(logEntry[1]));
            params.put("sample_rate", validateLong(logEntry[2]));
            params.put("data_buffers", validateInt(logEntry[3]));
            params.put("silence_buffers", validateInt(logEntry[4]));
            params.put("in_buffer_size", validateInt(logEntry[5]));
            params.put("out_buffer_size", validateInt(logEntry[6]));
            params.put("late_buffers", validateInt(logEntry[7]));
            params.put("avg_latency", validateInt(logEntry[8]));
            params.put("max_latency", validateInt(logEntry[9]));
            params.put("starts", validateInt(logEntry[10]));
            params.put("avg_start_latency", validateInt(logEntry[11]));
            params.put("max_start_latency", validateInt(logEntry[12]));
            params.put("delays", validateInt(logEntry[13]));
            params.put("overflows", validateInt(logEntry[14]));
            params.put("excess", validateInt(logEntry[15]));
            params.put("out_sample_rate", validateInt(logEntry[16]));

            super.doInBackground(voids);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Error parsing log file", e);
        }
        return null;
    }

    private String[] getRecentLogEntry() {
        String logLine = getLastLogLine();
        if (logLine == null) {
            Log.d(TAG, "No log line read");
            return null;
        }
        String[] tokens = logLine.split("\\s+");
        if (tokens.length != 17) {
            Log.d(TAG, "Incorrect log data received \"" + logLine + "\"");
            return null;
        }
        long timestamp = 0;
        try {
            timestamp = Long.parseLong(tokens[0]);
        } catch (NumberFormatException e) {
            Log.d(TAG, "Can't parse \"" + tokens[0] + "\" to integer");
        }

        long recordAge = System.currentTimeMillis() / 1000l - timestamp;

        if (recordAge > MAX_RECORD_AGE_S) {
            Log.d(TAG, "Log record outdated: " + recordAge + "s");
            return null;
        }
        return tokens;
    }

    private String getLastLogLine() {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getLogFile()));
            String lastLine = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("loaded ")) {
                    lastLine = line;
                }
            }
            return lastLine;
        } catch (IOException e) {
            Log.d(TAG, "Exception reading log", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    private File getLogFile() {
        File scrAudioDir = new File(context.getFilesDir(), AudioDriverInstaller.SCR_AUDIO_DIR);
        return new File(scrAudioDir, AudioDriverInstaller.MODULE_LOG);
    }

    private void truncateLog() {
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(getLogFile());
            fileWriter.close();
        } catch (IOException e) {
            Log.d(TAG, "Error truncating log file", e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String validateInt(String string) {
        Integer.parseInt(string);
        return string;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String validateLong(String string) {
        Long.parseLong(string);
        return string;
    }

    @Override
    protected String getUrl() {
        return BASE_URL;
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
