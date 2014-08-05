package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.audio.AudioDriver;

import java.io.File;

public class Settings {
    private static final String TAG = "scr_Settings";
    public static final int FFMPEG_MPEG_4_ENCODER = -2;
    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";
    private static final String AUDIO_SOURCE = "AUDIO_SOURCE";
    private static final String RESOLUTION_WIDTH = "RESOLUTION_WIDTH";
    private static final String RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT";
    private static final String FRAME_RATE = "FRAME_RATE";
    private static final String TRANSFORMATION = "TRANSFORMATION";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String VIDEO_BITRATE = "VIDEO_BITRATE";
    private static final String COLOR_FIX = "COLOR_FIX";
    private static final String HIDE_ICON = "HIDE_ICON";
    private static final String SHOW_TOUCHES = "SHOW_TOUCHES";
    private static final String STOP_ON_SCREEN_OFF = "STOP_ON_SCREEN_OFF";
    private static final String OUTPUT_DIR = "OUTPUT_DIR";
    private static final String OUTPUT_DIR_WRITABLE = "OUTPUT_DIR_WRITABLE";
    private static final String VIDEO_ENCODER = "VIDEO_ENCODER";
    private static final String VERTICAL_FRAMES = "VERTICAL_FRAMES";
    private static final String SHOW_UNSTABLE = "SHOW_UNSTABLE";
    private static final String SETTINGS_MODIFIED = "SETTINGS_MODIFIED";
    private static final String APP_VERSION = "APP_VERSION";
    private static final String BUILD_FINGERPRINT = "BUILD_FINGERPRINT";
    private static Settings instance;
    private SharedPreferences preferences;
    private AudioSource audioSource = AudioSource.MIC;
    private boolean temporaryMute = false;
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
    private int defaultVideoEncoder = MediaRecorder.VideoEncoder.H264;
    private boolean verticalFrames = false;
    private File outputDir;
    private File defaultOutputDir;
    private String outputDirName;
    private boolean outputDirWritable;
    private ShowTouchesController showTouchesController;
    private AudioDriver audioDriver;
    private boolean settingsModified;
    private int appVersion;
    private int previousAppVersion;
    private boolean appUpdated;
    private boolean systemUpdated;
    private DeviceProfile deviceProfile;
    private boolean showUnstable = true;

    private Settings(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        resolutionsManager = new ResolutionsManager(context);
        showTouchesController = new ShowTouchesController(context);
        audioDriver = new AudioDriver(context);
        appVersion = Utils.getAppVersion(context);
        outputDirName = context.getString(R.string.output_dir);
        defaultOutputDir = new File(Environment.getExternalStorageDirectory(), outputDirName);
        checkAppUpdate();
        checkSystemUpdate();
        new LoadDeviceProfileAsyncTask(this, context, appVersion, appUpdated, systemUpdated).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        // readPreferences(); will be called when device profile is loaded
        if (appUpdated) {
            handleAppUpdate();
        }
        validateOutputDir();
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new Settings(context.getApplicationContext());
        }
    }

    public static synchronized Settings getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Settings not initialized");
        }
        return instance;
    }

    private void readPreferences() {
        settingsModified = preferences.getBoolean(SETTINGS_MODIFIED, false);

        String audioSource = preferences.getString(AUDIO_SOURCE, AudioSource.MIC.name());
        this.audioSource = AudioSource.valueOf(audioSource);

        int resolutionWidth = preferences.getInt(RESOLUTION_WIDTH, -1);
        if (resolutionWidth != -1) {
            int resolutionHeight = preferences.getInt(RESOLUTION_HEIGHT, 0);
            resolution = resolutionsManager.getResolution(resolutionWidth, resolutionHeight);
        }

        frameRate = preferences.getInt(FRAME_RATE, 15);

        String transformation = preferences.getString(TRANSFORMATION, defaultTransformation.name());
        try {
            this.transformation = Transformation.valueOf(transformation);
        } catch (IllegalArgumentException e) {
            this.transformation = this.defaultTransformation;
        }

        String videoBitrate = preferences.getString(VIDEO_BITRATE, defaultVideoBitrate.name());
        this.videoBitrate = VideoBitrate.valueOf(videoBitrate);

        String samplingRate = preferences.getString(SAMPLING_RATE, defaultSamplingRate.name());
        this.samplingRate = SamplingRate.valueOf(samplingRate);

        colorFix = preferences.getBoolean(COLOR_FIX, defaultColorFix);

        hideIcon = preferences.getBoolean(HIDE_ICON, false);

        showTouches = preferences.getBoolean(SHOW_TOUCHES, false);

        stopOnScreenOff = preferences.getBoolean(STOP_ON_SCREEN_OFF, true);

        String outputDirPath = preferences.getString(OUTPUT_DIR, defaultOutputDir.getAbsolutePath());
        outputDir = new File(outputDirPath);
        outputDirWritable = preferences.getBoolean(OUTPUT_DIR_WRITABLE, false);

        videoEncoder = preferences.getInt(VIDEO_ENCODER, defaultVideoEncoder);

        verticalFrames = preferences.getBoolean(VERTICAL_FRAMES, false);

        showUnstable = preferences.getBoolean(SHOW_UNSTABLE, false);
    }

    private void checkAppUpdate() {
        previousAppVersion = preferences.getInt(APP_VERSION, -1);
        appUpdated = (previousAppVersion != -1 && previousAppVersion != appVersion);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(APP_VERSION, appVersion);
        editor.commit();
    }

    private void checkSystemUpdate() {
        String previousFingerprint = preferences.getString(BUILD_FINGERPRINT, null);
        String fingerprint = Build.FINGERPRINT;
        systemUpdated = (previousFingerprint != null && !previousFingerprint.equals(fingerprint));
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BUILD_FINGERPRINT, fingerprint);
        editor.commit();
    }

    private void handleAppUpdate() {
        SharedPreferences.Editor editor = preferences.edit();

        if (previousAppVersion <= 24) {
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

    public void updateDefaults() {
        if (deviceProfile == null) return;

        boolean updated = false;

        if (deviceProfile.getDefaultResolution() != null
                && deviceProfile.getDefaultResolution() != defaultResolution) {
            updated = true;
            defaultResolution = deviceProfile.getDefaultResolution();
        }

        if (deviceProfile.getDefaultTransformation() != null
                && deviceProfile.getDefaultTransformation() != defaultTransformation) {
            updated = true;
            defaultTransformation = deviceProfile.getDefaultTransformation();
        }

        if (deviceProfile.getDefaultVideoBitrate() != null
                && deviceProfile.getDefaultVideoBitrate() != defaultVideoBitrate) {
            updated = true;
            defaultVideoBitrate = deviceProfile.getDefaultVideoBitrate();
        }

        if (deviceProfile.getDefaultSamplingRate() != null
                && deviceProfile.getDefaultSamplingRate() != defaultSamplingRate) {
            updated = true;
            defaultSamplingRate = deviceProfile.getDefaultSamplingRate();
        }

        if (deviceProfile.getDefaultColorFix() != defaultColorFix) {
            updated = true;
            defaultColorFix = deviceProfile.getDefaultColorFix();
        }

        if (deviceProfile.getDefaultVideoEncoder() != 0
                && deviceProfile.getDefaultVideoEncoder() != defaultVideoEncoder) {
            updated = true;
            defaultVideoEncoder = deviceProfile.getDefaultVideoEncoder();
        }

        if (updated) {
            readPreferences(); // refresh preferences to restore update defaults
        }
    }

    private void settingsModified(SharedPreferences.Editor editor) {
        if (!settingsModified) {
            settingsModified = true;
            editor.putBoolean(SETTINGS_MODIFIED, true);
        }
        editor.commit();
    }

    public boolean areSettingsModified() {
        return settingsModified;
    }

    public AudioSource getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
        if (audioDriver.shouldInstall()) {
            audioDriver.install();
        } else if (audioSource != AudioSource.INTERNAL && audioDriver.shouldUninstall()) {
            audioDriver.uninstall();
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AUDIO_SOURCE, audioSource.name());
        settingsModified(editor);
    }

    public boolean getTemporaryMute() {
        return temporaryMute;
    }

    public void setTemporaryMute(boolean temporaryMute) {
        this.temporaryMute = temporaryMute;
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
            settingsModified(editor);
        }
    }

    public ResolutionsManager getResolutionsManager() {
        return resolutionsManager;
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
        settingsModified(editor);
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TRANSFORMATION, transformation.name());
        settingsModified(editor);
    }

    public SamplingRate getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(SamplingRate samplingRate) {
        this.samplingRate = samplingRate;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SAMPLING_RATE, samplingRate.name());
        settingsModified(editor);
    }

    public VideoBitrate getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(VideoBitrate videoBitrate) {
        this.videoBitrate = videoBitrate;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(VIDEO_BITRATE, videoBitrate.name());
        settingsModified(editor);
    }

    public boolean getColorFix() {
        return colorFix;
    }

    public void setColorFix(boolean colorFix) {
        this.colorFix = colorFix;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(COLOR_FIX, colorFix);
        settingsModified(editor);
    }

    public boolean getHideIcon() {
        return hideIcon;
    }

    public void setHideIcon(boolean hideIcon) {
        this.hideIcon = hideIcon;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(HIDE_ICON, hideIcon);
        settingsModified(editor);
    }

    public boolean getShowTouches() {
        return showTouches;
    }

    public void setShowTouches(boolean showTouches) {
        this.showTouches = showTouches;
        showTouchesController.setShowTouches(showTouches);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_TOUCHES, showTouches);
        settingsModified(editor);
    }

    public boolean getStopOnScreenOff() {
        return stopOnScreenOff;
    }

    public void setStopOnScreenOff(boolean stopOnScreenOff) {
        this.stopOnScreenOff = stopOnScreenOff;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(STOP_ON_SCREEN_OFF, stopOnScreenOff);
        settingsModified(editor);
    }

    public int getVideoEncoder() {
        return videoEncoder;
    }

    public void setVideoEncoder(int videoEncoder) {
        if (Utils.isX86() && videoEncoder == FFMPEG_MPEG_4_ENCODER) {
            Log.w(TAG, "Software encoder is not supported on x86 platform, resetting to H264");
            videoEncoder = MediaRecorder.VideoEncoder.H264;
        }
        this.videoEncoder = videoEncoder;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(VIDEO_ENCODER, videoEncoder);
        settingsModified(editor);
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
        settingsModified(editor);
    }

    public File getDefaultOutputDir() {
        return defaultOutputDir;
    }

    public void setVerticalFrames(boolean verticalFrames) {
        this.verticalFrames = verticalFrames;

        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(VERTICAL_FRAMES, verticalFrames);
        settingsModified(editor);
    }

    public boolean getVerticalFrames() {
        return verticalFrames;
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
        editor.remove(VIDEO_BITRATE);

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

        videoEncoder = defaultVideoEncoder;
        editor.remove(VIDEO_ENCODER);

        verticalFrames = false;
        editor.remove(VERTICAL_FRAMES);

        settingsModified = false;
        editor.remove(SETTINGS_MODIFIED);

        editor.commit();
    }

    public boolean currentEqualsDefault() {
        return audioSource == AudioSource.MIC
                && getResolution() == getDefaultResolution()
                && frameRate == 15
                && transformation == defaultTransformation
                && samplingRate == defaultSamplingRate
                && videoBitrate == defaultVideoBitrate
                && colorFix == defaultColorFix
                && !hideIcon
                && !showTouches
                && stopOnScreenOff
                && outputDir.equals(defaultOutputDir)
                && videoEncoder == defaultVideoEncoder
                && !verticalFrames;
    }

    public boolean coreEqualsDefault() {
        return getResolution() == getDefaultResolution()
                && transformation == defaultTransformation
                && samplingRate == defaultSamplingRate
                && videoBitrate == defaultVideoBitrate
                && colorFix == defaultColorFix
                && videoEncoder == defaultVideoEncoder;
    }

    public boolean statsBasedDefaults() {
        return deviceProfile != null;
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

    public AudioDriver getAudioDriver() {
        return audioDriver;
    }

    public void setDeviceProfile(DeviceProfile deviceProfile) {
        this.deviceProfile = deviceProfile;
        if (deviceProfile != null) {
            updateDefaults();
        } else {
            readPreferences();
        }
    }

    public DeviceProfile getDeviceProfile() {
        return deviceProfile;
    }

    public boolean getShowUnstable() {
        return showUnstable;
    }

    public void setShowUnstable(boolean showUnstable) {
        this.showUnstable = showUnstable;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_UNSTABLE, showUnstable);
        editor.commit();
    }
}


