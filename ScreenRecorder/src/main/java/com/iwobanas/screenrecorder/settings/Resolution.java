package com.iwobanas.screenrecorder.settings;

public class Resolution {
    private int labelId;
    private int width;
    private int height;
    private int paddingWidth;
    private int paddingHeight;

    public Resolution(int labelId, int width, int height) {
        this(labelId, width, height, 0, 0);
    }

    public Resolution(int labelId, int width, int height, int paddingWidth, int paddingHeight) {
        this.labelId = labelId;
        this.width = width;
        this.height = height;
        this.paddingWidth = paddingWidth;
        this.paddingHeight = paddingHeight;
    }

    public int getLabelId() {
        return labelId;
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
