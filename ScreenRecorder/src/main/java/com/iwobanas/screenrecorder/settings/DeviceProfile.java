package com.iwobanas.screenrecorder.settings;

import android.media.MediaRecorder;
import android.os.Build;

import com.iwobanas.screenrecorder.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class DeviceProfile {

    public static final String BLACKLISTS = "blacklists";
    private static final String VIDEO_ENCODER = "video_encoder";
    private static final String RESOLUTION_WIDTH = "resolution_width";
    private static final String RESOLUTION_HEIGHT = "resolution_height";
    private static final String TRANSFORMATION = "transformation";
    private static final String VIDEO_BITRATE = "video_bitrate";
    private static final String COLOR_FIX = "color_fix";
    private static final String SAMPLING_RATE = "sampling_rate";
    private static final String DEFAULTS = "defaults";
    public static final String RESOLUTION = "resolution";
    private ResolutionsManager resolutionsManager;

    private int defaultVideoEncoder;
    private Resolution defaultResolution;
    private Transformation defaultTransformation;
    private VideoBitrate defaultVideoBitrate;
    private SamplingRate defaultSamplingRate;
    private boolean defaultColorFix;

    private Collection<Integer> hideVideoEncoders = new ArrayList<Integer>(3);
    private Collection<Resolution> hideResolutions = new ArrayList<Resolution>();
    private Collection<Transformation> hideTransformations = new ArrayList<Transformation>();
    private Collection<VideoBitrate> hideVideoBitrates = new ArrayList<VideoBitrate>();

    private ArrayList<Integer> stableVideoEncoders;
    private ArrayList<Transformation> stableTransformations;

    public DeviceProfile(JSONObject json, ResolutionsManager resolutionsManager) throws JSONException {
        this.resolutionsManager = resolutionsManager;
        if (json.has(DEFAULTS)) {
            decodeDefaults(json.getJSONObject(DEFAULTS));
        }

        if (json.has(BLACKLISTS)) {
            decodeBlacklists(json.getJSONObject(BLACKLISTS));
        }
        validateBlackLists();
        validateDefaults();
    }

    private void decodeDefaults(JSONObject defaults) throws JSONException {
        if (defaults.has(VIDEO_ENCODER)) {
            defaultVideoEncoder = defaults.getInt(VIDEO_ENCODER);
        }
        if (defaults.has(RESOLUTION_WIDTH) && defaults.has(RESOLUTION_HEIGHT)) {
            int w = defaults.getInt(RESOLUTION_WIDTH);
            int h = defaults.getInt(RESOLUTION_HEIGHT);
            defaultResolution = resolutionsManager.getResolution(w, h);
        }
        if (defaults.has(TRANSFORMATION)) {
            try {
                defaultTransformation = Transformation.valueOf(defaults.getString(TRANSFORMATION));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (defaults.has(VIDEO_BITRATE)) {
            defaultVideoBitrate = VideoBitrate.getByBitrate(defaults.getInt(VIDEO_BITRATE));
        }
        if (defaults.has(COLOR_FIX)) {
            defaultColorFix = defaults.getBoolean(COLOR_FIX);
        }
        if (defaults.has(SAMPLING_RATE)) {
            defaultSamplingRate = SamplingRate.getBySamplingRate(defaults.getInt(SAMPLING_RATE));
        }
    }

    private void decodeBlacklists(JSONObject blacklists) throws JSONException {
        if (blacklists.has(VIDEO_ENCODER)) {
            JSONArray encoders = blacklists.getJSONArray(VIDEO_ENCODER);
            for (int i = 0; i < encoders.length(); i++) {
                hideVideoEncoders.add(encoders.getInt(i));
            }
        }
        if (blacklists.has(RESOLUTION)) {
            JSONArray resolutions = blacklists.getJSONArray(RESOLUTION);
            for (int i = 0; i < resolutions.length(); i++) {
                JSONObject resolution = resolutions.getJSONObject(i);
                hideResolutions.add(
                        resolutionsManager.getResolution(
                                resolution.getInt(RESOLUTION_WIDTH),
                                resolution.getInt(RESOLUTION_HEIGHT)
                        )
                );
            }
        }
        if (blacklists.has(TRANSFORMATION)) {
            JSONArray transformations = blacklists.getJSONArray(TRANSFORMATION);
            for (int i = 0; i < transformations.length(); i++) {
                hideTransformations.add(Transformation.valueOf(transformations.getString(i)));
            }
        }
        if (blacklists.has(VIDEO_BITRATE)) {
            JSONArray bitRates = blacklists.getJSONArray(VIDEO_BITRATE);
            for (int i = 0; i < bitRates.length(); i++) {
                hideVideoBitrates.add(VideoBitrate.getByBitrate(bitRates.getInt(i)));
            }
        }
    }

    private void validateBlackLists() {
        Integer[] allEncoders = Utils.isX86() ?
                new Integer[]{MediaRecorder.VideoEncoder.H264, MediaRecorder.VideoEncoder.MPEG_4_SP}
                : new Integer[]{MediaRecorder.VideoEncoder.H264, Settings.FFMPEG_MPEG_4_ENCODER, MediaRecorder.VideoEncoder.MPEG_4_SP};
        stableVideoEncoders = new ArrayList<Integer>(allEncoders.length);

        for (Integer encoder : allEncoders) {
            if (!hideVideoEncoder(encoder))
                stableVideoEncoders.add(encoder);
        }

        if (stableVideoEncoders.size() == 0) {
            hideVideoEncoders.clear();
            for (Integer encoder : allEncoders) {
                stableVideoEncoders.add(encoder);
            }
        }

        Transformation[] allTransformations = Build.VERSION.SDK_INT < 18 ?
                new Transformation[]{Transformation.CPU, Transformation.GPU}
                : new Transformation[]{Transformation.CPU, Transformation.GPU, Transformation.OES};
        stableTransformations = new ArrayList<Transformation>(allTransformations.length);

        for (Transformation transformation : allTransformations) {
            if (!hideTransformation(transformation))
                stableTransformations.add(transformation);
        }

        if (stableTransformations.size() == 0) {
            hideTransformations.clear();
            for (Transformation transformation : allTransformations) {
                stableTransformations.add(transformation);
            }
        }
    }

    private void validateDefaults() {
        // this is just a safety check. with a sane stats default settings should never be blacklisted

        if (hideVideoEncoder(defaultVideoEncoder)) {
            defaultVideoEncoder = stableVideoEncoders.get(0);
        }

        if (hideTransformation(defaultTransformation)) {
            defaultTransformation = stableTransformations.get(0);
        }
    }

    public int getDefaultVideoEncoder() {
        return defaultVideoEncoder;
    }

    public Resolution getDefaultResolution() {
        return defaultResolution;
    }

    public Transformation getDefaultTransformation() {
        return defaultTransformation;
    }

    public VideoBitrate getDefaultVideoBitrate() {
        return defaultVideoBitrate;
    }

    public SamplingRate getDefaultSamplingRate() {
        return defaultSamplingRate;
    }

    public boolean getDefaultColorFix() {
        return defaultColorFix;
    }

    public boolean hideResolution(Resolution resolution) {
        return hideResolutions.contains(resolution);
    }

    public boolean hideVideoEncoder(Integer encoder) {
        return hideVideoEncoders.contains(encoder);
    }

    public boolean hideTransformation(Transformation transformation) {
        return hideTransformations.contains(transformation);
    }

    public boolean hideVideoBitrate(VideoBitrate bitrate) {
        return hideVideoBitrates.contains(bitrate);
    }

    public ArrayList<Integer> getStableVideoEncoders() {
        return stableVideoEncoders;
    }

    public ArrayList<Transformation> getStableTransformations() {
        return stableTransformations;
    }
}
