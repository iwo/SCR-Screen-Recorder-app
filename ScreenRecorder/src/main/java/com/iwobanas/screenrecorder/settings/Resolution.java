package com.iwobanas.screenrecorder.settings;

public class Resolution {
    private String label;
    private int width;
    private int height;

    public Resolution(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
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
}
