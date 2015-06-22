package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.iwobanas.screenrecorder.BuildConfig;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.audio.AudioDriver;

import java.io.File;

public class Settings {
    private static final String TAG = "scr_Settings";
    private static final String RESOLUTION_WIDTH = "RESOLUTION_WIDTH";
    private static final String RESOLUTION_HEIGHT = "RESOLUTION_HEIGHT";
    private static final String ORIENTATION = "ORIENTATION";
    private static final String TIME_LAPSE = "TIME_LAPSE";
    private static final String FRAME_RATE = "FRAME_RATE";
    private static final String TRANSFORMATION = "TRANSFORMATION";
    private static final String SAMPLING_RATE = "SAMPLING_RATE";
    private static final String INTERNAL_SAMPLING_RATE = "INTERNAL_SAMPLING_RATE";
    private static final String STEREO = "STEREO";
    private static final String INTERNAL_STEREO = "INTERNAL_STEREO";
    private static final String MIC_GAIN = "MIC_GAIN";
    private static final String VIDEO_BITRATE = "VIDEO_BITRATE";
    private static final String COLOR_FIX = "COLOR_FIX";
    private static final String HIDE_ICON = "HIDE_ICON";
    private static final String SHOW_TOUCHES = "SHOW_TOUCHES";
    private static final String STOP_ON_SCREEN_OFF = "STOP_ON_SCREEN_OFF";
    private static final String OUTPUT_DIR = "OUTPUT_DIR";
    private static final String OUTPUT_DIR_WRITABLE = "OUTPUT_DIR_WRITABLE";
    private static final String DOCUMENT_DIR_URI = "DOCUMENT_DIR_URI";
    private static final String DOCUMENT_DIR_NAME = "DOCUMENT_DIR_NAME";
    private static final String VIDEO_ENCODER = "VIDEO_ENCODER";
    private static final String VERTICAL_FRAMES = "VERTICAL_FRAMES";
    private static final String SHOW_UNSTABLE = "SHOW_UNSTABLE";
    private static final String SHOW_ADVANCED = "SHOW_ADVANCED";
    private static final String DISABLE_AUDIO_WARNING = "DISABLE_AUDIO_WARNING";
    private static final String SETTINGS_MODIFIED = "SETTINGS_MODIFIED";
    private static final String APP_VERSION = "APP_VERSION";
    private static final String BUILD_FINGERPRINT = "BUILD_FINGERPRINT";

    private static final String PREFERENCES_NAME = "ScreenRecorderSettings";

    public static final String AUDIO_SOURCE = "AUDIO_SOURCE";
    public static final String SHOW_CAMERA = "SHOW_CAMERA";
    public static final String CAMERA_ALPHA = "CAMERA_ALPHA";
    public static final String ROOT_ENABLED = "ROOT_ENABLED";

    private static Settings instance;
    private SharedPreferences preferences;
    private AudioSource audioSource = AudioSource.MIC;
    private boolean temporaryMute = false;
    private Resolution resolution;
    private Resolution defaultResolution;
    private ResolutionsManager resolutionsManager;
    private Orientation orientation = Orientation.AUTO;
    private int timeLapse = 1;
    private int defaultFrameRate = 30;
    private int frameRate = defaultFrameRate;
    private Transformation transformation = Transformation.GPU;
    private Transformation defaultTransformation = Transformation.GPU;
    private SamplingRate defaultSamplingRate = SamplingRate.SAMPLING_RATE_16_KHZ;
    private SamplingRate samplingRate = SamplingRate.SAMPLING_RATE_16_KHZ;
    private SamplingRate internalSamplingRate = null;
    private boolean stereo = false;
    private boolean internalStereo = true;
    private int micGain = 2;
    private VideoBitrate defaultVideoBitrate = VideoBitrate.BITRATE_10_MBPS;
    private VideoBitrate videoBitrate = VideoBitrate.BITRATE_10_MBPS;
    private boolean colorFix = false;
    private boolean defaultColorFix = false;
    private boolean hideIcon = false;
    private boolean showTouches = false;
    private boolean showCamera = false;
    private float cameraAlpha = 1.0f;
    private boolean stopOnScreenOff = true;
    private int videoEncoder = VideoEncoder.H264;
    private int defaultVideoEncoder = VideoEncoder.H264;
    private boolean verticalFrames = false;
    private File outputDir;
    private File defaultOutputDir;
    private Uri documentDirUri;
    private String documentDirName;
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
    private boolean showUnstable = false;
    private boolean showAdvanced = false;
    private boolean disableAudioWarning = false;
    private boolean rootEnabled = true;
    private boolean rootFlavor = true;

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
        rootFlavor = BuildConfig.FLAVOR.equals("root");
        if (isRootFlavor()) {
            loadDeviceProfileIfNeeded(context);
            // readPreferences(); will be called when device profile is loaded
        } else {
            readPreferences();
        }
        if (appUpdated) {
            handleAppUpdate();
        }
        validateOutputDir();
    }

    public static synchronized void initialize(Context context) {
        if (instance == null) {
            instance = new Settings(context.getApplicationContext());
        } else {
            instance.loadDeviceProfileIfNeeded(context.getApplicationContext());
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

        String orientation = preferences.getString(ORIENTATION, Orientation.AUTO.name());
        try {
            this.orientation = Orientation.valueOf(orientation);
        } catch (IllegalArgumentException e) {
            this.orientation = Orientation.LANDSCAPE;
        }

        timeLapse = preferences.getInt(TIME_LAPSE, 1);

        frameRate = preferences.getInt(FRAME_RATE, defaultFrameRate);

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

        String internalSamplingRate = preferences.getString(INTERNAL_SAMPLING_RATE, null);
        if (internalSamplingRate != null) {
            this.internalSamplingRate = SamplingRate.valueOf(internalSamplingRate);
        }

        stereo = preferences.getBoolean(STEREO, false);
        internalStereo = preferences.getBoolean(INTERNAL_STEREO, true);

        micGain = preferences.getInt(MIC_GAIN, 2);

        colorFix = preferences.getBoolean(COLOR_FIX, defaultColorFix);

        hideIcon = preferences.getBoolean(HIDE_ICON, false);

        showTouches = preferences.getBoolean(SHOW_TOUCHES, false);

        showCamera = preferences.getBoolean(SHOW_CAMERA, false);
        cameraAlpha = preferences.getFloat(CAMERA_ALPHA, 1.0f);

        stopOnScreenOff = preferences.getBoolean(STOP_ON_SCREEN_OFF, true);

        String outputDirPath = preferences.getString(OUTPUT_DIR, defaultOutputDir.getAbsolutePath());
        outputDir = new File(outputDirPath);
        outputDirWritable = preferences.getBoolean(OUTPUT_DIR_WRITABLE, false);

        String documentDir = preferences.getString(DOCUMENT_DIR_URI, null);
        documentDirUri = documentDir == null ? null : Uri.parse(documentDir);

        documentDirName = preferences.getString(DOCUMENT_DIR_NAME, null);

        videoEncoder = preferences.getInt(VIDEO_ENCODER, defaultVideoEncoder);

        verticalFrames = preferences.getBoolean(VERTICAL_FRAMES, false);

        showAdvanced = preferences.getBoolean(SHOW_ADVANCED, false);
        showUnstable = preferences.getBoolean(SHOW_UNSTABLE, false);
        disableAudioWarning = preferences.getBoolean(DISABLE_AUDIO_WARNING, false);
        rootEnabled = preferences.getBoolean(ROOT_ENABLED, true);
    }

    private void loadDeviceProfileIfNeeded(Context context) {
        if (isRootFlavor() && deviceProfile == null) {
            new LoadDeviceProfileAsyncTask(this, context, appVersion, appUpdated, systemUpdated).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void checkAppUpdate() {
        previousAppVersion = preferences.getInt(APP_VERSION, -1);
        appUpdated = (previousAppVersion != -1 && previousAppVersion != appVersion);
        preferences.edit().putInt(APP_VERSION, appVersion).commit();
    }

    private void checkSystemUpdate() {
        String previousFingerprint = preferences.getString(BUILD_FINGERPRINT, null);
        String fingerprint = Build.FINGERPRINT;
        systemUpdated = (previousFingerprint != null && !previousFingerprint.equals(fingerprint));
        preferences.edit().putString(BUILD_FINGERPRINT, fingerprint).commit();
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

        if (previousAppVersion <= 66) {
            if (frameRate == 15) {
                editor.remove(FRAME_RATE);
                frameRate = defaultFrameRate;
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
        if (!isRootEnabled() && audioSource != AudioSource.MUTE) {
            return AudioSource.MIC;
        }
        return audioSource;
    }

    public void setAudioSource(AudioSource audioSource) {
        this.audioSource = audioSource;
        if (!audioSource.getRequiresDriver() && audioDriver.shouldUninstall()) {
            audioDriver.uninstall();
        }
        settingsModified(preferences.edit().putString(AUDIO_SOURCE, audioSource.name()));
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
            settingsModified(preferences.edit()
                            .putInt(RESOLUTION_WIDTH, resolution.getWidth())
                            .putInt(RESOLUTION_HEIGHT, resolution.getHeight())
            );
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

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        settingsModified(preferences.edit().putString(ORIENTATION, orientation.name()));
    }

    public int getTimeLapse() {
        return timeLapse;
    }

    public void setTimeLapse(int timeLapse) {
        this.timeLapse = timeLapse;
        settingsModified(preferences.edit().putInt(TIME_LAPSE, timeLapse));
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int value) {
        frameRate = value;
        settingsModified(preferences.edit().putInt(FRAME_RATE, frameRate));
    }

    public Transformation getTransformation() {
        return transformation;
    }

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
        settingsModified(preferences.edit().putString(TRANSFORMATION, transformation.name()));
    }

    public SamplingRate getSamplingRate() {
        if (getAudioSource().getRequiresDriver()) {
            if (internalSamplingRate == null) {
                if (audioDriver.getSamplingRate() > 0) {
                    return SamplingRate.getBySamplingRate(audioDriver.getSamplingRate());
                }
                return SamplingRate.SAMPLING_RATE_44_KHZ;
            }
            return internalSamplingRate;
        } else {
            return samplingRate;
        }
    }

    public void setSamplingRate(SamplingRate samplingRate) {
        if (getAudioSource().getRequiresDriver()) {
            this.internalSamplingRate = samplingRate;
            settingsModified(preferences.edit().putString(INTERNAL_SAMPLING_RATE, internalSamplingRate.name()));
        } else {
            this.samplingRate = samplingRate;
            settingsModified(preferences.edit().putString(SAMPLING_RATE, samplingRate.name()));
        }
    }


    public boolean getStereo() {
        if (getAudioSource().getRequiresDriver()) {
            return internalStereo;
        } else {
            return stereo;
        }
    }

    public void setStereo(boolean stereo) {
        if (getAudioSource().getRequiresDriver()) {
            this.internalStereo = stereo;
            settingsModified(preferences.edit().putBoolean(INTERNAL_STEREO, stereo));
        } else {
            this.stereo = stereo;
            settingsModified(preferences.edit().putBoolean(STEREO, stereo));
        }
        this.stereo = stereo;
    }

    public int getMicGain() {
        return micGain;
    }

    public void setMicGain(int micGain) {
        this.micGain = micGain;
        settingsModified(preferences.edit().putInt(MIC_GAIN, micGain));
    }

    public VideoBitrate getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(VideoBitrate videoBitrate) {
        this.videoBitrate = videoBitrate;
        settingsModified(preferences.edit().putString(VIDEO_BITRATE, videoBitrate.name()));
    }

    public boolean getColorFix() {
        return colorFix;
    }

    public void setColorFix(boolean colorFix) {
        this.colorFix = colorFix;
        settingsModified(preferences.edit().putBoolean(COLOR_FIX, colorFix));
    }

    public boolean getHideIcon() {
        return hideIcon;
    }

    public void setHideIcon(boolean hideIcon) {
        this.hideIcon = hideIcon;
        settingsModified(preferences.edit().putBoolean(HIDE_ICON, hideIcon));
    }

    public boolean getShowTouches() {
        return showTouches;
    }

    public void setShowTouches(boolean showTouches) {
        this.showTouches = showTouches;
        showTouchesController.setShowTouches(showTouches);
        settingsModified(preferences.edit().putBoolean(SHOW_TOUCHES, showTouches));
    }

    public boolean getShowCamera() {
        return showCamera;
    }

    public void setShowCamera(boolean showCamera) {
        this.showCamera = showCamera;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_CAMERA, showCamera);
        settingsModified(editor);
    }

    public float getCameraAlpha() {
        return cameraAlpha;
    }

    public void setCameraAlpha(float cameraAlpha) {
        this.cameraAlpha = cameraAlpha;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(CAMERA_ALPHA, cameraAlpha);
        settingsModified(editor);
    }

    public boolean getStopOnScreenOff() {
        return stopOnScreenOff;
    }

    public void setStopOnScreenOff(boolean stopOnScreenOff) {
        this.stopOnScreenOff = stopOnScreenOff;
        settingsModified(preferences.edit().putBoolean(STOP_ON_SCREEN_OFF, stopOnScreenOff));
    }

    public int getVideoEncoder() {
        if (!isRootEnabled() && !VideoEncoder.isNoRoot(videoEncoder)) {
            return VideoEncoder.getNoRootVariant(videoEncoder);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && VideoEncoder.isNoRoot(videoEncoder)) {
            return VideoEncoder.getRootVariant(videoEncoder);
        }
        return videoEncoder;
    }

    public void setVideoEncoder(int videoEncoder) {
        if (Utils.isX86() && VideoEncoder.isSoftware(videoEncoder)) {
            Log.w(TAG, "Software encoder is not supported on x86 platform, resetting to H264");
            videoEncoder = VideoEncoder.H264;
        }
        this.videoEncoder = videoEncoder;
        settingsModified(preferences.edit().putInt(VIDEO_ENCODER, videoEncoder));
    }

    public File getOutputDir() {
        if (outputDir == null) {
            return defaultOutputDir;
        }
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
        settingsModified(preferences.edit().putString(OUTPUT_DIR, outputDir.getAbsolutePath()));
    }

    public File getDefaultOutputDir() {
        return defaultOutputDir;
    }

    public Uri getDocumentDirUri() {
        return documentDirUri;
    }

    public void setDocumentDirUri(Uri documentDirUri) {
        this.documentDirUri = documentDirUri;
        settingsModified(preferences.edit().putString(DOCUMENT_DIR_URI, documentDirUri == null ? null : documentDirUri.toString()));
    }

    public String getDocumentDirName() {
        return documentDirName;
    }

    public void setDocumentDirName(String documentDirName) {
        this.documentDirName = documentDirName;
        settingsModified(preferences.edit().putString(DOCUMENT_DIR_NAME, documentDirName));
    }

    public void setVerticalFrames(boolean verticalFrames) {
        this.verticalFrames = verticalFrames;
        settingsModified(preferences.edit().putBoolean(VERTICAL_FRAMES, verticalFrames));
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

        orientation = Orientation.AUTO;
        editor.remove(ORIENTATION);

        timeLapse = 1;
        editor.remove(TIME_LAPSE);

        frameRate = defaultFrameRate;
        editor.remove(FRAME_RATE);

        transformation = defaultTransformation;
        editor.remove(TRANSFORMATION);

        samplingRate = defaultSamplingRate;
        editor.remove(SAMPLING_RATE);

        internalSamplingRate = null;
        editor.remove(INTERNAL_SAMPLING_RATE);

        stereo = false;
        editor.remove(STEREO);

        internalStereo = true;
        editor.remove(INTERNAL_STEREO);

        micGain = 2;
        editor.remove(MIC_GAIN);

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

        showCamera = false;
        editor.remove(SHOW_CAMERA);

        cameraAlpha = 1.0f;
        editor.remove(CAMERA_ALPHA);

        stopOnScreenOff = true;
        editor.remove(STOP_ON_SCREEN_OFF);

        outputDir = defaultOutputDir;
        editor.remove(OUTPUT_DIR);

        documentDirUri = null;
        editor.remove(DOCUMENT_DIR_URI);

        documentDirName = null;
        editor.remove(DOCUMENT_DIR_NAME);

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
                && frameRate == defaultFrameRate
                && transformation == defaultTransformation
                && samplingRate == defaultSamplingRate
                && videoBitrate == defaultVideoBitrate
                && colorFix == defaultColorFix
                && timeLapse == 1
                && !hideIcon
                && !showTouches
                && stopOnScreenOff
                && defaultOutputDir.equals(outputDir)
                && videoEncoder == defaultVideoEncoder
                && !verticalFrames;
    }

    public boolean coreEqualsDefault() {
        return getResolution() == getDefaultResolution()
                && transformation == defaultTransformation
                && samplingRate == defaultSamplingRate
                && videoBitrate == defaultVideoBitrate
                && colorFix == defaultColorFix
                && timeLapse == 1
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
        if (this.deviceProfile != null && deviceProfile == null)
            return;

        this.deviceProfile = deviceProfile;
        if (deviceProfile != null) {
            updateDefaults();
            preventLockedUnstable();
        } else {
            readPreferences();
        }
    }

    public DeviceProfile getDeviceProfile() {
        return deviceProfile;
    }

    private void preventLockedUnstable() {
        if (deviceProfile != null && !showUnstable) {
            if (deviceProfile.getStableVideoEncoders().size() == 1 && deviceProfile.hideVideoEncoder(videoEncoder)) {
                setVideoEncoder(deviceProfile.getStableVideoEncoders().get(0));
            }
            if (deviceProfile.getStableTransformations().size() == 1 && deviceProfile.hideTransformation(transformation)) {
                setTransformation(deviceProfile.getStableTransformations().get(0));
            }
        }
    }

    public boolean getShowUnstable() {
        return showUnstable;
    }

    public void setShowUnstable(boolean showUnstable) {
        this.showUnstable = showUnstable;
        preferences.edit().putBoolean(SHOW_UNSTABLE, showUnstable).commit();
        preventLockedUnstable();
    }

    public boolean getShowAdvanced() {
        return showAdvanced;
    }

    public void setShowAdvanced(boolean showAdvanced) {
        this.showAdvanced = showAdvanced;
        preferences.edit().putBoolean(SHOW_ADVANCED, showAdvanced).commit();
    }

    public void setDisableAudioWarning(boolean disableAudioWarning) {
        this.disableAudioWarning = disableAudioWarning;
        preferences.edit().putBoolean(DISABLE_AUDIO_WARNING, disableAudioWarning).apply();
    }

    public boolean getDisableAudioWarning() {
        return disableAudioWarning;
    }

    public boolean isNoRootVideoEncoder() {
        return !isRootEnabled() || VideoEncoder.isNoRoot(getVideoEncoder());
    }

    public boolean isRootFlavor() {
        return rootFlavor;
    }

    public boolean isRootEnabled() {
        return rootFlavor && rootEnabled;
    }

    public void setRootEnabled(boolean rootEnabled) {
        this.rootEnabled = rootEnabled;
        preferences.edit().putBoolean(ROOT_ENABLED, rootEnabled).commit();
    }

    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void updateAudioDriverConfig() {
        if (audioSource.getRequiresDriver()) {
            audioDriver.updateConfig(getAudioSource() == AudioSource.MIX, getMicGain());
        }
    }
}


