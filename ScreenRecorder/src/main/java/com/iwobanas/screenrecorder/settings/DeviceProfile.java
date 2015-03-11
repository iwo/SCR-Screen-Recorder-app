package com.iwobanas.screenrecorder.settings;

import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DeviceProfile {

    private static final String TAG = "scr_DeviceProfile";
    private static final String BLACKLISTS = "blacklists";
    private static final String RESOLUTION = "resolution";
    private static final String VIDEO_ENCODER = "video_encoder";
    private static final String RESOLUTION_WIDTH = "resolution_width";
    private static final String RESOLUTION_HEIGHT = "resolution_height";
    private static final String TRANSFORMATION = "transformation";
    private static final String VIDEO_BITRATE = "video_bitrate";
    private static final String COLOR_FIX = "color_fix";
    private static final String SAMPLING_RATE = "sampling_rate";
    private static final String DEFAULTS = "defaults";
    private static final String VIDEO_CONFIGS = "video_configs";
    private static final String FRAME_RATE = "frame_rate";
    private static final String STABILITY = "stability";
    private static final String INTERNAL_AUDIO_STABLE = "internal_audio_stable";
    private ResolutionsManager resolutionsManager;

    private int defaultVideoEncoder;
    private Resolution defaultResolution;
    private Transformation defaultTransformation;
    private VideoBitrate defaultVideoBitrate;
    private SamplingRate defaultSamplingRate;
    private boolean defaultColorFix;
    private boolean internalAudioStable;

    private Collection<Integer> hideVideoEncoders = new ArrayList<Integer>(3);
    private Collection<Resolution> hideResolutions = new ArrayList<Resolution>();
    private Collection<Transformation> hideTransformations = new ArrayList<Transformation>();
    private Collection<VideoBitrate> hideVideoBitrates = new ArrayList<VideoBitrate>();

    private List<VideoConfig> videoConfigs = new ArrayList<VideoConfig>();

    private List<Integer> stableVideoEncoders;
    private List<Transformation> stableTransformations;

    public DeviceProfile(JSONObject json, ResolutionsManager resolutionsManager) throws JSONException {
        this.resolutionsManager = resolutionsManager;
        if (json.has(DEFAULTS) && !json.isNull(DEFAULTS)) {
            decodeDefaults(json.getJSONObject(DEFAULTS));
        }

        if (json.has(BLACKLISTS) && !json.isNull(BLACKLISTS)) {
            decodeBlacklists(json.getJSONObject(BLACKLISTS));
        }

        if (json.has(VIDEO_CONFIGS) && !json.isNull(VIDEO_CONFIGS)) {
            decodeVideoConfigs(json.getJSONArray(VIDEO_CONFIGS));
        }

        internalAudioStable = !json.has(INTERNAL_AUDIO_STABLE) || json.getBoolean(INTERNAL_AUDIO_STABLE);

        validateBlackLists();
        validateDefaults();
    }

    private void decodeVideoConfigs(JSONArray jsonConfigs) {
        videoConfigs = new ArrayList<VideoConfig>(jsonConfigs.length());

        for (int i = 0; i < jsonConfigs.length(); i++) {
            try {
                JSONObject jsonConfig = jsonConfigs.getJSONObject(i);
                Resolution resolution = resolutionsManager.getResolution(
                        jsonConfig.getInt(RESOLUTION_WIDTH),
                        jsonConfig.getInt(RESOLUTION_HEIGHT)
                );
                if (resolution == null)
                    continue;

                videoConfigs.add(
                        new VideoConfig(
                                jsonConfig.getInt(VIDEO_ENCODER),
                                resolution,
                                Transformation.valueOf(jsonConfig.getString(TRANSFORMATION)),
                                VideoBitrate.getByBitrate(jsonConfig.getInt(VIDEO_BITRATE)),
                                jsonConfig.getDouble(FRAME_RATE),
                                jsonConfig.getInt(STABILITY))
                );
            } catch (Exception e) {
                Log.w(TAG, "Error parsing video configs", e);
            }
        }
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
        Integer[] allEncoders = VideoEncoder.getAllSupportedEncoders(false);
        stableVideoEncoders = new ArrayList<Integer>(allEncoders.length);

        for (Integer encoder : allEncoders) {
            if (!hideVideoEncoder(encoder))
                stableVideoEncoders.add(encoder);
        }

        if (stableVideoEncoders.size() == 0) {
            hideVideoEncoders.clear();
            stableVideoEncoders = Arrays.asList(allEncoders);
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
            stableTransformations = Arrays.asList(allTransformations);
        }
    }

    private void validateDefaults() {
        // this is just a safety check. with a sane stats default settings should never be blacklisted

        if (hideVideoEncoder(defaultVideoEncoder) || (Utils.isX86() && VideoEncoder.isSoftware(defaultVideoEncoder))) {
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

    public List<Integer> getStableVideoEncoders() {
        return stableVideoEncoders;
    }

    public List<Transformation> getStableTransformations() {
        return stableTransformations;
    }

    public List<VideoConfig> getVideoConfigs() {
        return videoConfigs;
    }

    public boolean isHighEndDevice() {
        return videoConfigs.size() > 0 && videoConfigs.get(0).getFrameRate() > 15.0;
    }

    public boolean isInternalAudioStable() {
        return internalAudioStable;
    }
}
