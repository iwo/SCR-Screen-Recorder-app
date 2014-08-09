package com.iwobanas.screenrecorder.settings;

public class VideoConfig {
    private int videoEncoder;
    private Resolution resolution;
    private Transformation transformation;
    private VideoBitrate videoBitrate;

    private double frameRate;

    public VideoConfig(int videoEncoder, Resolution resolution, Transformation transformation, VideoBitrate videoBitrate, double frameRate) {
        this.videoEncoder = videoEncoder;
        this.resolution = resolution;
        this.transformation = transformation;
        this.videoBitrate = videoBitrate;
        this.frameRate = frameRate;
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
}
