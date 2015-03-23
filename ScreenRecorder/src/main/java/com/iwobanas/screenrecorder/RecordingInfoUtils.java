package com.iwobanas.screenrecorder;

import android.os.Looper;
import android.util.Log;

import com.iwobanas.videorepair.mp4.Mp4;
import com.iwobanas.videorepair.mp4.atoms.Atom;
import com.iwobanas.videorepair.mp4.atoms.MdatAtom;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RecordingInfoUtils {

    private static final String TAG = "scr_RecordingInfoUtils";
    private static final long MIN_MDAT_SIZE_LIMIT = 512 * 1024; // 0.5MiB

    public static void updateInfoIfNeeded(RecordingProcessState state, RecordingInfo recordingInfo) {
        if (recordingInfo != null && recordingInfo.file != null && (state.isError()
                || state == RecordingProcessState.FINISHED
                || state == RecordingProcessState.MAX_FILE_SIZE_REACHED)) {

            long size = recordingInfo.file.length();
            recordingInfo.size = (int) (size / 1024);

            if (recordingInfo.file.exists() && size > 0) {
                checkFormatValidity(recordingInfo);
            }
        }
    }

    private static void checkFormatValidity(RecordingInfo recordingInfo) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            Utils.logStackTrace(TAG, "This shouldn't be called from main thread!");
        }

        RandomAccessFile file = null;
        try {
            if (!recordingInfo.file.exists()) {
                recordingInfo.formatValidity = RecordingInfo.FormatValidity.NO_FILE;
            } else if (recordingInfo.file.length() == 0) {
                recordingInfo.formatValidity = RecordingInfo.FormatValidity.EMPTY;
            } else {
                file = new RandomAccessFile(recordingInfo.file, "r");
                Mp4 mp4 = new Mp4(file, true);

                Atom mdat = mp4.get(MdatAtom.NAME);
                if (mp4.isValid()) {
                    recordingInfo.formatValidity = RecordingInfo.FormatValidity.VALID;
                } else if (mdat != null && mdat.getSize() > MIN_MDAT_SIZE_LIMIT) {
                    recordingInfo.formatValidity = RecordingInfo.FormatValidity.INTERRUPTED;
                } else if (mdat != null) {
                    recordingInfo.formatValidity = RecordingInfo.FormatValidity.NO_DATA;
                } else {
                    recordingInfo.formatValidity = RecordingInfo.FormatValidity.UNRECOGNISED;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file format", e);
            recordingInfo.formatValidity = RecordingInfo.FormatValidity.UNKNOWN;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
