package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;

import java.io.File;

public class Settings {
    public static final int FFMPEG_MPEG_4_ENCODER = -2;
    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";
    private static final String AUDIO_SOURCE = "AUDIO_SOURCE";
    private static final String RESOLUTION_WIDTH = "RESOLUTION_WIDTH";
    private static final String RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT";
    private static final String FRAME_RATE = "FRAME_RATE";
    private static final String TRANSFORMATION = "TRANSFORMATION";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String VIDO_BITRATE = "VIDO_BITRATE";
    private static final String COLOR_FIX = "COLOR_FIX";
    private static final String HIDE_ICON = "HIDE_ICON";
    private static final String SHOW_TOUCHES = "SHOW_TOUCHES";
    private static final String STOP_ON_SCREEN_OFF = "STOP_ON_SCREEN_OFF";
    private static final String OUTPUT_DIR = "OUTPUT_DIR";
    private static final String OUTPUT_DIR_WRITABLE = "OUTPUT_DIR_WRITABLE";
    private static final String VIDEO_ENCODER = "VIDEO_ENCODER";
    private static final String DEFAULT_RESOLUTION_WIDTH = "DEFAULT_RESOLUTION_WIDTH";
    private static final String DEFAULT_RESOLUTION_HEIGHT = "DEFAULT_RESOLUTION_HEIGHT";
    private static final String DEFAULT_TRANSFORMATION = "DEFAULT_TRANSFORMATION";
    private static final String DEFAULT_SAMPLING_RATE = "DEFAULT_SAMPLING_RATE";
    private static final String DEFAULT_VIDO_BITRATE = "DEFAULT_VIDO_BITRATE";
    private static final String DEFAULT_COLOR_FIX = "DEFAULT_COLOR_FIX";
    private static final String DEFAULTS_UPDATE_TIMESTAMP = "DEFAULTS_UPDATE_TIMESTAMP";
    private static final String APP_VERSION = "APP_VERSION";
    private static Settings instance;
    private SharedPreferences preferences;
    private AudioSource audioSource = AudioSource.MIC;
    private Resolution resolution;
    private Resolution defaultResolution;
    private ResolutionsManager resolutionsManager;
    private int frameRate = 15;
    private Transformation transformation = Transformation.GPU;
    private Transformation defaultTransformation = Transformation.GPU;
    private SamplingRate defaultSamplingRate = SamplingRate.SAMPLING_RATE_16_KHZ;
    private SamplingRate samplingRate = SamplingRate.SAMPLING_RATE_16_KHZ;
    private VideoBitrate defaultVideoBitrate = VideoBitrate.BITRATE_10_MBPS;
    private VideoBitrate videoBitrate = VideoBitrate.BITRATE_10_MBPS;
    private boolean colorFix = false;
    private boolean defaultColorFix = false;
    private boolean hideIcon = false;
    private boolean showTouches = false;
    private boolean stopOnScreenOff = true;
    private int videoEncoder = MediaRecorder.VideoEncoder.H264;
    private File outputDir;
    private File defaultOutputDir;
    private String outputDirName;
    private boolean outputDirWritable;
    private ShowTouchesController showTouchesController;
    private int appVersion;
    private boolean appUpdated;

    private Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        resolutionsManager = new ResolutionsManager(context);
        showTouchesController = new ShowTouchesController(context);
        appVersion = Utils.getAppVersion(context);
        outputDirName = context.getString(R.string.output_dir);
        defaultOutputDir = new File(Environment.getExternalStorageDirectory(), outputDirName);
        readPreferences();
        handleUpdate();
        if (shouldUpdateDefaults()) {
            new LoadDefaultsAsyncTask(appVersion).execute();
        }
        validateOutputDir();
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new Settings(context);
        }
    }

    public static synchronized Settings getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Settings not initialized");
        }
        return instance;
    }

    private void readPreferences() {
        String audioSource = preferences.getString(AUDIO_SOURCE, AudioSource.MIC.name());
        this.audioSource = AudioSource.valueOf(audioSource);

        int defaultResolutionWidth = preferences.getInt(DEFAULT_RESOLUTION_WIDTH, -1);
        if (defaultResolutionWidth != -1) {
            int defaultResolutionHeight = preferences.getInt(DEFAULT_RESOLUTION_HEIGHT, 0);
            defaultResolution = resolutionsManager.getResolution(defaultResolutionWidth, defaultResolutionHeight);
        }

        int resolutionWidth = preferences.getInt(RESOLUTION_WIDTH, -1);
        if (resolutionWidth != -1) {
            int resolutionHeight = preferences.getInt(RESOLUTION_HEIGHT, 0);
            resolution = resolutionsManager.getResolution(resolutionWidth, resolutionHeight);
        }

        frameRate = preferences.getInt(FRAME_RATE, 15);

        String defaultTransformation = preferences.getString(DEFAULT_TRANSFORMATION, Transformation.GPU.name());
        try {
            this.defaultTransformation = Transformation.valueOf(defaultTransformation);
        } catch (IllegalArgumentException e) {
            this.defaultTransformation = Transformation.GPU;
        }

        String transformation = preferences.getString(TRANSFORMATION, defaultTransformation);
        try {
            this.transformation = Transformation.valueOf(transformation);
        } catch (IllegalArgumentException e) {
            this.transformation = this.defaultTransformation;
        }

        String defaultVideoBitrate = preferences.getString(DEFAULT_VIDO_BITRATE, VideoBitrate.BITRATE_10_MBPS.name());
        this.defaultVideoBitrate = VideoBitrate.valueOf(defaultVideoBitrate);

        String videoBitrate = preferences.getString(DEFAULT_VIDO_BITRATE, defaultVideoBitrate);
        this.videoBitrate = VideoBitrate.valueOf(videoBitrate);


        String defaultSamplingRate = preferences.getString(DEFAULT_SAMPLING_RATE, SamplingRate.SAMPLING_RATE_16_KHZ.name());
        this.defaultSamplingRate = SamplingRate.valueOf(defaultSamplingRate);

        String samplingRate = preferences.getString(SAMPLING_RATE, defaultSamplingRate);
        this.samplingRate = SamplingRate.valueOf(samplingRate);

        defaultColorFix = preferences.getBoolean(DEFAULT_COLOR_FIX, false);
        colorFix = preferences.getBoolean(COLOR_FIX, defaultColorFix);

        hideIcon = preferences.getBoolean(HIDE_ICON, false);

        showTouches = preferences.getBoolean(SHOW_TOUCHES, false);

        stopOnScreenOff = preferences.getBoolean(STOP_ON_SCREEN_OFF, true);

        String outputDirPath = preferences.getString(OUTPUT_DIR, defaultOutputDir.getAbsolutePath());
        outputDir = new File(outputDirPath);
        outputDirWritable = preferences.getBoolean(OUTPUT_DIR_WRITABLE, false);

        videoEncoder = preferences.getInt(VIDEO_ENCODER, MediaRecorder.VideoEncoder.H264);
    }

    private void handleUpdate() {
        int previousVersion = preferences.getInt(APP_VERSION, -1);
        if (previousVersion == -1 || appVersion == previousVersion) return;

        appUpdated = true;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(APP_VERSION, appVersion);

        if (appVersion == 17) {
            if (transformation == Transformation.GPU && Build.VERSION.SDK_INT >= 18) {
                editor.remove(TRANSFORMATION);
                transformation = Transformation.OES;
            }
        }

        if (previousVersion <= 24) {
            if (preferences.contains(SHOW_TOUCHES)) {
                if (!preferences.getBoolean(SHOW_TOUCHES, true)) {
                    showTouchesController.setShowTouches(false);
                }
            } else {
                showTouches = true;
                editor.putBoolean(SHOW_TOUCHES, true);
            }
        }

        editor.commit();
    }

    private void validateOutputDir() {

        if (outputDirWritable) return; // Skip validation if it passed before

        SharedPreferences.Editor editor = preferences.edit();
        if (Utils.checkDirWritable(defaultOutputDir)) {
            editor.putBoolean(OUTPUT_DIR_WRITABLE, true);
        } else {
            boolean usingDefault = defaultOutputDir.equals(getOutputDir());
            // fallback to legacy path /sdcard
            defaultOutputDir = new File("/sdcard", outputDirName);
            if (usingDefault) {
                outputDir = defaultOutputDir;
                editor.remove(OUTPUT_DIR);
            }
        }
        editor.commit();
    }

    public void updateDefaults(String resolutionWidth, String resolutionHeight, String transformation,
                               String videoBitrate, String samplingRate, String colorFix) {
        if (resolutionWidth != null && resolutionWidth.length() > 0 &&
            resolutionHeight != null && resolutionHeight.length() > 0) {
            int w = Integer.parseInt(resolutionWidth);
            int h = Integer.parseInt(resolutionHeight);
            defaultResolution = resolutionsManager.getResolution(w, h);
        }
        if (transformation != null && transformation.length() > 0) {
            try {
                defaultTransformation = Transformation.valueOf(transformation);
            } catch (IllegalArgumentException ignored) {}
        }
        if (videoBitrate != null && videoBitrate.length() > 0) {
            for (VideoBitrate bitrate : VideoBitrate.values()) {
                if (bitrate.getCommand().equals(videoBitrate)) {
                    defaultVideoBitrate = bitrate;
                    break;
                }
            }
        }
        if (samplingRate != null && samplingRate.length() > 0) {
            for (SamplingRate rate : SamplingRate.values()) {
                if (rate.getCommand().equals(samplingRate)) {
                    defaultSamplingRate = rate;
                    break;
                }
            }
        }
        if (colorFix != null && colorFix.length() > 0) {
            defaultColorFix = Boolean.valueOf(colorFix);
        }

        saveDefaults();
        readPreferences(); // refresh preferences to restore update defaults
    }

    private void saveDefaults() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(DEFAULTS_UPDATE_TIMESTAMP, System.currentTimeMillis());
        if (defaultResolution != null) {
            editor.putInt(DEFAULT_RESOLUTION_WIDTH, defaultResolution.getWidth());
            editor.putInt(DEFAULT_RESOLUTION_HEIGHT, defaultResolution.getHeight());
        } else {
            editor.remove(DEFAULT_RESOLUTION_WIDTH);
            editor.remove(DEFAULT_RESOLUTION_HEIGHT);
        }

        if (defaultTransformation != null) {
            editor.putString(DEFAULT_TRANSFORMATION, defaultTransformation.name());
        } else {
            editor.remove(DEFAULT_TRANSFORMATION);
        }

        editor.putBoolean(DEFAULT_COLOR_FIX, defaultColorFix);

        editor.commit();
    }

    private Boolean shouldUpdateDefaults() {
        if (appUpdated) return true;
        long time = System.currentTimeMillis() - preferences.getLong(DEFAULTS_UPDATE_TIMESTAMP, 0);
        return (time > 24 * 60 * 60 * 1000);
    }

    public AudioSource getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AUDIO_SOURCE, audioSource.name());
        editor.commit();
    }

    public Resolution getResolution() {
        if (resolution == null)
            return getDefaultResolution();

        return resolution;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
        if (resolution != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(RESOLUTION_WIDTH, resolution.getWidth());
            editor.putInt(RESOLUTION_HEIGHT, resolution.getHeight());
            editor.commit();
        }
    }

    public Resolution[] getResolutions() {
        return resolutionsManager.getResolutions();
    }

    public Resolution getDefaultResolution() {
        if (defaultResolution != null) {
            return defaultResolution;
        }
        return resolutionsManager.getDefaultResolution();
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int value) {
        frameRate = value;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(FRAME_RATE, frameRate);
        editor.commit();
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TRANSFORMATION, transformation.name());
        editor.commit();
    }

    public SamplingRate getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(SamplingRate samplingRate) {
        this.samplingRate = samplingRate;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SAMPLING_RATE, samplingRate.name());
        editor.commit();
    }

    public VideoBitrate getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(VideoBitrate videoBitrate) {
        this.videoBitrate = videoBitrate;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(VIDO_BITRATE, videoBitrate.name());
        editor.commit();
    }

    public boolean getColorFix() {
        return colorFix;
    }

    public void setColorFix(boolean colorFix) {
        this.colorFix = colorFix;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(COLOR_FIX, colorFix);
        editor.commit();
    }

    public boolean getHideIcon() {
        return hideIcon;
    }

    public void setHideIcon(boolean hideIcon) {
        this.hideIcon = hideIcon;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(HIDE_ICON, hideIcon);
        editor.commit();
    }

    public boolean getShowTouches() {
        return showTouches;
    }

    public void setShowTouches(boolean showTouches) {
        this.showTouches = showTouches;
        showTouchesController.setShowTouches(showTouches);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_TOUCHES, showTouches);
        editor.commit();
    }

    public boolean getStopOnScreenOff() {
        return stopOnScreenOff;
    }

    public void setStopOnScreenOff(boolean stopOnScreenOff) {
        this.stopOnScreenOff = stopOnScreenOff;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(STOP_ON_SCREEN_OFF, stopOnScreenOff);
        editor.commit();
    }

    public int getVideoEncoder() {
        return videoEncoder;
    }

    public void setVideoEncoder(int videoEncoder) {
        this.videoEncoder = videoEncoder;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(VIDEO_ENCODER, videoEncoder);
        editor.commit();
    }

    public File getOutputDir() {
        if (outputDir == null) {
            return defaultOutputDir;
        }
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(OUTPUT_DIR, outputDir.getAbsolutePath());
        editor.commit();
    }

    public File getDefaultOutputDir() {
        return defaultOutputDir;
    }

    public void restoreDefault() {
        SharedPreferences.Editor editor = preferences.edit();

        audioSource = AudioSource.MIC;
        editor.remove(AUDIO_SOURCE);

        resolution = getDefaultResolution();
        editor.remove(RESOLUTION_WIDTH);
        editor.remove(RESOLUTION_HEIGHT);

        frameRate = 15;
        editor.remove(FRAME_RATE);

        transformation = defaultTransformation;
        editor.remove(TRANSFORMATION);

        samplingRate = defaultSamplingRate;
        editor.remove(SAMPLING_RATE);

        videoBitrate = defaultVideoBitrate;
        editor.remove(VIDO_BITRATE);

        colorFix = defaultColorFix;
        editor.remove(COLOR_FIX);

        hideIcon = false;
        editor.remove(HIDE_ICON);

        if (showTouches) {
            showTouchesController.setShowTouches(false);
            showTouches = false;
        }
        editor.remove(SHOW_TOUCHES);

        stopOnScreenOff = true;
        editor.remove(STOP_ON_SCREEN_OFF);

        outputDir = defaultOutputDir;
        editor.remove(OUTPUT_DIR);

        videoEncoder = MediaRecorder.VideoEncoder.H264;
        editor.remove(VIDEO_ENCODER);

        editor.commit();
    }

    public void restoreShowTouches() {
        if (showTouches) {
            showTouchesController.setShowTouches(false);
        }
    }

    public void applyShowTouches() {
        if (showTouches) {
            showTouchesController.setShowTouches(true);
        }
    }
}


