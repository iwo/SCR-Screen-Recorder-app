package com.iwobanas.screenrecorder;

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

    public static enum FormatValidity {
        VALID,
        INTERRUPTED,
        UNRECOGNISED,
        NO_DATA,
        EMPTY,
        UNKNOWN
    }
}
