package com.iwobanas.screenrecorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.iwobanas.screenrecorder.settings.Resolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ResolutionsManager {

    private int width = 0;
    private int height = 0;
    private Resolution[] resolutions;
    private Resolution defaultResolution;

    private int[] standardHeights = new int[]{1080, 720, 480, 360, 240};
    private int[] standardWidths = new int[]{1080, 720, 480};

    private String original = "Max";
    private String half = "Half";

    @SuppressLint("NewApi")
    public ResolutionsManager(Context context) {
        Display display = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        height = Math.min(metrics.heightPixels, metrics.widthPixels);
        width = Math.max(metrics.heightPixels, metrics.widthPixels);

        generateResolutions();
    }

    private void generateResolutions() {
        double aspectRatio = (double) width / (double) height;

        Set<Integer> heightsSet = new HashSet<Integer>();

        ArrayList<Resolution> resolutions = new ArrayList<Resolution>(10);

        for (int i = 0; i < standardHeights.length; i++) {
            int h = standardHeights[i];
            int w = nearestEven(h * aspectRatio);
            String label = null;

            if (h > height)
                continue;

            if (h == height) {
                w = width;
                label = original;
            } else if (h == height / 2) {
                label = half;
            } else {
                label = h + "p";
            }
            heightsSet.add(h);
            Resolution resolution = new Resolution(label, w, h);
            if (h == 480 && w != 820) { // 819 is a special case (now changed to 820 - left for backward compatibility) for 1024x600 which crashes media recorder
                defaultResolution = resolution;
            }
            resolutions.add(resolution);
        }

        for (int i = 0; i < standardWidths.length; i++) {
            int w = standardWidths[i];
            int h = nearestEven(w / aspectRatio);
            String label = w + "p↦";

            if (h > height || heightsSet.contains(h))
                continue;

            resolutions.add(new Resolution(label, w, h));
        }

        if (!heightsSet.contains(height)) {
            Resolution resolution = new Resolution(original, width, height);
            if (defaultResolution == null) {
                defaultResolution = resolution;
            }
            resolutions.add(resolution);
        }
        if (!heightsSet.contains(nearestEven(height / 2.0))) {
            resolutions.add(new Resolution(half, nearestEven(width / 2.0), nearestEven(height / 2.0)));
        }

        Collections.sort(resolutions, new Comparator<Resolution>() {
            @Override
            public int compare(Resolution a, Resolution b) {
                return b.getHeight() - a.getHeight();
            }
        });

        this.resolutions = resolutions.toArray(new Resolution[resolutions.size()]);
    }

    private int nearestEven(double value) {
        return (int) (2.0 * Math.round(value / 2.0));
    }

    public Resolution getDefaultResolution() {
        return defaultResolution;
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
        return new Resolution(Math.min(width, height) + "p", width, height);
    }
}
