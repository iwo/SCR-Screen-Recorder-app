package com.iwobanas.screenrecorder.settings;

import java.util.Arrays;
import java.util.List;

public enum SamplingRate {
    SAMPLING_RATE_8_KHZ(8000, "8kHz"),
    SAMPLING_RATE_11_KHZ(11025, "11.025kHz"),
    SAMPLING_RATE_12_KHZ(12000, "12kHz"),
    SAMPLING_RATE_16_KHZ(16000, "16kHz"),
    SAMPLING_RATE_22_KHZ(22050, "22.05kHz"),
    SAMPLING_RATE_24_KHZ(24000, "24kHz"),
    SAMPLING_RATE_32_KHZ(32000, "32kHz"),
    SAMPLING_RATE_44_KHZ(44100, "44.1kHz"),
    SAMPLING_RATE_48_KHZ(48000, "48kHz"),
    SAMPLING_RATE_96_KHZ(96000, "96kHz");

    public static final List<SamplingRate> STANDARD = Arrays.asList(
            SAMPLING_RATE_16_KHZ,
            SAMPLING_RATE_32_KHZ,
            SAMPLING_RATE_44_KHZ,
            SAMPLING_RATE_48_KHZ
    );

    private int samplingRate;
    private String label;

    SamplingRate(int samplingRate, String label) {
        this.samplingRate = samplingRate;
        this.label = label;
    }

    public static SamplingRate getBySamplingRate(int samplingRate) {
        for (SamplingRate rate : values()) {
            if (rate.samplingRate == samplingRate) {
                return rate;
            }
        }
        throw new IllegalArgumentException("Invalid sampling rate: " + samplingRate);
    }

    public String getCommand() {
        return String.valueOf(samplingRate);
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public String getLabel() {
        return label;
    }
}
