package com.iwobanas.screenrecorder.settings;

public class VideoConfig {
    private int videoEncoder;
    private Resolution resolution;
    private Transformation transformation;
    private VideoBitrate videoBitrate;

    private double frameRate;
    private int stability;

    public VideoConfig(int videoEncoder, Resolution resolution, Transformation transformation, VideoBitrate videoBitrate, double frameRate, int stability) {
        this.videoEncoder = videoEncoder;
        this.resolution = resolution;
        this.transformation = transformation;
        this.videoBitrate = videoBitrate;
        this.frameRate = frameRate;
        this.stability = stability;
    }

    public int getVideoEncoder() {
        return videoEncoder;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public VideoBitrate getVideoBitrate() {
        return videoBitrate;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public int getStability() {
        return stability;
    }
}
