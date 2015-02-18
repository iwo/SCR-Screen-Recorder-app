package com.iwobanas.screenrecorder;

public enum RecordingProcessState {
    NEW,
    INITIALIZING,
    READY,
    STARTING,
    RECORDING,
    STOPPING,
    PROCESSING,
    FINISHED,
    DESTROYED,

    MAX_FILE_SIZE_REACHED,


    SU_ERROR(true, true),
    CPU_NOT_SUPPORTED_ERROR(true, true),
    INSTALLATION_ERROR(true, true),
    UNKNOWN_STARTUP_ERROR(true, true),

    VIDEO_CODEC_ERROR(true),
    UNKNOWN_RECORDING_ERROR(true),
    MEDIA_RECORDER_ERROR(true),
    OUTPUT_FILE_ERROR(true),
    MICROPHONE_BUSY_ERROR(true),
    OPEN_GL_ERROR(true),
    SECURE_SURFACE_ERROR(true),
    AUDIO_CONFIG_ERROR(true),
    SELINUX_ERROR(true),;

    private boolean error;
    private boolean critical;

    RecordingProcessState() {
        this(false);
    }

    RecordingProcessState(boolean error) {
        this(error, false);
    }

    RecordingProcessState(boolean error, boolean critical) {
        this.error = error;
        this.critical = critical;
    }

    public boolean isError() {
        return error;
    }

    public boolean isCritical() {
        return critical;
    }
}
