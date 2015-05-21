package com.iwobanas.screenrecorder;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Resolution;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.File;
import java.io.FileOutputStream;

public class LogSettingsAsyncTask extends AsyncTask <Void, Void, Void> {
    private static final String TAG = "scr_LogSettingsAT";
    private final RecordingInfo recordingInfo;
    private final int width;
    private final int height;
    private final int videoEncoder;
    private final int samplingRate;

    public LogSettingsAsyncTask(RecordingInfo recordingInfo) {
        this.recordingInfo = recordingInfo;
        Settings s = Settings.getInstance();
        Resolution res = s.getResolution();
        width = res.getWidth();
        height = res.getHeight();
        videoEncoder = s.getVideoEncoder();
        samplingRate = s.getAudioSource() == AudioSource.MUTE ? 0 : s.getSamplingRate().getSamplingRate();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            String logLine = recordingInfo.file.getAbsolutePath() + "|" + recordingInfo.formatValidity.getCode() + "|"
                    + width
                    + " " + height
                    + " " + videoEncoder
                    + " " + samplingRate
                    + " " + recordingInfo.rotation
                    + " " + recordingInfo.adjustedRotation
                    + "\n";
            File outputDir = new File(Environment.getExternalStorageDirectory(), "ScreenRecorder");
            File logFile = new File(outputDir, "video_info.txt");
            FileOutputStream outputStream = new FileOutputStream(logFile, true);
            outputStream.write(logLine.getBytes());
            outputStream.close();
        } catch (Exception e) {
            Log.w(TAG, "Error saving recording info", e);
        }
        return null;
    }
}
