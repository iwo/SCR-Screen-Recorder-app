package com.iwobanas.screenrecorder;

import android.net.Uri;

import java.io.File;

public class RecordingInfo {
    public File file;
    public String rotation;
    public volatile int exitValue = -1;
    public int size = 0;
    public int time = 0;
    public float fps = -1f;
    public int rotateView;
    public int verticalInput;
    public int adjustedRotation;
    public FormatValidity formatValidity = FormatValidity.UNKNOWN;
    public boolean useDocument;
    public Uri documentUri;

    public static enum FormatValidity {
        VALID("V"),
        INTERRUPTED("I"),
        UNRECOGNISED("U"),
        NO_DATA("N"),
        NO_FILE("F"),
        EMPTY("E"),
        UNKNOWN("X");

        private final String code;

        FormatValidity(String code) {

            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
