package com.iwobanas.screenrecorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.iwobanas.screenrecorder.settings.Resolution;

public class ResolutionsManager {

    private boolean isPortrait;
    private int width = 0;
    private int height = 0;
    private Resolution[] resolutions;
    private Resolution defaultResolution;

    private int[] standardHeights = new int[]{1080, 720, 480, 360, 240};

    private String original = "Original";
    private String half = "Half";

    @SuppressLint("NewApi")
    public ResolutionsManager(Context context) {
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Configuration config = context.getResources().getConfiguration();
        isPortrait = Utils.getDeviceDefaultOrientation(display, config) == Configuration.ORIENTATION_PORTRAIT;
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        height = Math.min(metrics.heightPixels, metrics.widthPixels);
        width = Math.max(metrics.heightPixels, metrics.widthPixels);

        generateResolutions();
    }

    private void generateResolutions() {
        double aspectRatio = (double) width / (double) height;
        boolean gotOriginal = false;
        boolean gotHalf = false;

        ArrayList<Resolution> resolutions = new ArrayList<Resolution>(10);

        for (int i = 0; i < standardHeights.length; i++) {
            int h = standardHeights[i];
            int w = 0;
            String label = null;

            if (h > height)
                continue;

            if (h == height) {
                w = width;
                label = original;
                gotOriginal = true;
            } else if (h == height / 2) {
                label = half;
                w = width / 2;
                gotHalf = true;
            } else {
                label = h + "p";
                w = (int) (h * aspectRatio);
            }

            resolutions.add(newResolution(label, w, h));
        }

        if (!gotOriginal) {
            resolutions.add(newResolution(original, width, height));
        }
        if (!gotHalf) {
            resolutions.add(newResolution(half, width / 2, height / 2));
        }

        Collections.sort(resolutions, new Comparator<Resolution>() {
            @Override
            public int compare(Resolution a, Resolution b) {
                return b.getHeight() - a.getHeight();
            }
        });

        this.resolutions = resolutions.toArray(new Resolution[resolutions.size()]);
    }

    private Resolution newResolution(String label, int videoWidth, int videoHeight) {
        if (isPortrait)
            return new Resolution(label, videoHeight, videoWidth);

        return new Resolution(label, videoWidth, videoHeight);
    }

    public Resolution getDefaultResolution() {
        return new Resolution("Default", 0, 0);
    }

    public Resolution[] getResolutions() {
        return resolutions;
    }

    public Resolution getResolution(int width, int height) {
        for (Resolution resolution : resolutions) {
            if (resolution.getWidth() == width && resolution.getHeight() == height) {
                return resolution;
            }
        }
        return newResolution(Math.min(width, height) + "p", width, height);
    }
}
