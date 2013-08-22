package com.iwobanas.screenrecorder.settings;

public class Resolution {
    private String label;
    private int width;
    private int height;
    private int paddingWidth;
    private int paddingHeight;

    public Resolution(String label, int width, int height) {
        this(label, width, height, 0, 0);
    }

    public Resolution(String label, int width, int height, int paddingWidth, int paddingHeight) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.paddingWidth = paddingWidth;
        this.paddingHeight = paddingHeight;
    }

    public String getLabel() {
        return label;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPaddingWidth() {
        return paddingWidth;
    }

    public int getPaddingHeight() {
        return paddingHeight;
    }

    public int getVideoWidth() {
        return width - 2 * paddingWidth;
    }

    public int getVideoHeight() {
        return height - 2 * paddingHeight;
    }
}
