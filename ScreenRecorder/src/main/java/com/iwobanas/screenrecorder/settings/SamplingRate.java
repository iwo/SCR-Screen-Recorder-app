package com.iwobanas.screenrecorder.settings;

public enum SamplingRate {
    SAMPLING_RATE_8_KHZ(8000, "8kHz"),
    SAMPLING_RATE_16_KHZ(16000, "16kHz"),
    SAMPLING_RATE_32_KHZ(32000, "32kHz"),
    SAMPLING_RATE_44_KHZ(44100, "41.1kHz"),
    SAMPLING_RATE_48_KHZ(48000, "48kHz"),
    SAMPLING_RATE_96_KHZ(96000, "96kHz");

    private int samplingRate;
    private String label;

    SamplingRate(int samplingRate, String label) {
        this.samplingRate = samplingRate;
        this.label = label;
    }

    public String getCommand() {
        return String.valueOf(samplingRate);
    }

    public String getLabel() {
        return label;
    }
}
