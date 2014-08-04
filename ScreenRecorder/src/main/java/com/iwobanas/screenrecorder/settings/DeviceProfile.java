package com.iwobanas.screenrecorder.settings;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceProfile {

    private ResolutionsManager resolutionsManager;

    private int defaultVideoEncoder;
    private Resolution defaultResolution;
    private Transformation defaultTransformation;
    private VideoBitrate defaultVideoBitrate;
    private SamplingRate defaultSamplingRate;
    private boolean defaultColorFix;

    public DeviceProfile(JSONObject json, ResolutionsManager resolutionsManager) throws JSONException {
        this.resolutionsManager = resolutionsManager;
        if (json.has("defaults")) {
            decodeDefaults(json.getJSONObject("defaults"));
        }
    }

    private void decodeDefaults(JSONObject defaults) throws JSONException {
        if (defaults.has("video_encoder")) {
            defaultVideoEncoder = defaults.getInt("video_encoder");
        }
        if (defaults.has("resolution_width") && defaults.has("resolution_height")) {
            int w = defaults.getInt("resolution_width");
            int h = defaults.getInt("resolution_height");
            defaultResolution = resolutionsManager.getResolution(w, h);
        }
        if (defaults.has("transformation")) {
            try {
                defaultTransformation = Transformation.valueOf(defaults.getString("transformation"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (defaults.has("video_bitrate")) {
            String videoBitrate = defaults.getString("video_bitrate");
            for (VideoBitrate bitrate : VideoBitrate.values()) {
                if (bitrate.getCommand().equals(videoBitrate)) {
                    defaultVideoBitrate = bitrate;
                    break;
                }
            }
        }
        if (defaults.has("color_fix")) {
            defaultColorFix = defaults.getBoolean("color_fix");
        }
        if (defaults.has("sampling_rate")) {
            String samplingRate = defaults.getString("sampling_rate");
            for (SamplingRate rate : SamplingRate.values()) {
                if (rate.getCommand().equals(samplingRate)) {
                    defaultSamplingRate = rate;
                    break;
                }
            }
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
}
