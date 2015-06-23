package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.support.v4.provider.DocumentFile;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.iwobanas.screenrecorder.DirectoryChooserActivity;
import com.iwobanas.screenrecorder.GusherDialogFragment;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.RecorderService;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.audio.AudioDriver;
import com.iwobanas.screenrecorder.audio.AudioWarningDialogFragment;
import com.iwobanas.screenrecorder.audio.InstallationStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, AudioDriver.OnInstallListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_COPYRIGHTS_STATEMENT = "copyrights_statement";
    public static final String KEY_DONATE = "donate";
    public static final String KEY_NO_ROOT_MODE = "no_root_mode";
    public static final String KEY_VIDEO = "video";
    public static final String KEY_VIDEO_CONFIG = "video_config";
    public static final String KEY_VIDEO_ENCODER = "video_encoder";
    public static final String KEY_RESOLUTION = "resolution";
    public static final String KEY_ORIENTATION = "orientation";
    public static final String KEY_TIME_LAPSE = "time_lapse";
    public static final String KEY_TRANSFORMATION = "transformation";
    public static final String KEY_VIDEO_BITRATE = "video_bitrate";
    public static final String KEY_FRAME_RATE = "frame_rate";
    public static final String KEY_VERTICAL_FRAMES = "vertical_frames";
    public static final String KEY_AUDIO = "audio";
    public static final String KEY_AUDIO_SOURCE = "audio_source";
    public static final String KEY_SAMPLING_RATE = "sampling_rate";
    public static final String KEY_STEREO = "stereo";
    public static final String KEY_MIC_GAIN = "mic_gain";
    public static final String KEY_OTHER = "other";
    public static final String KEY_SHOW_CAMERA = "show_camera";
    public static final String KEY_CAMERA_ALPHA = "camera_alpha";
    public static final String KEY_HIDE_ICON = "hide_icon";
    public static final String KEY_SHOW_TOUCHES = "show_touches";
    public static final String KEY_DOCUMENT_DIR = "document_dir";
    public static final String KEY_OUTPUT_DIR = "output_dir";
    public static final String KEY_STOP_ON_SCREEN_OFF = "stop_on_screen_off";
    public static final String KEY_COLOR_FIX = "color_fix";
    public static final String KEY_GUSHER = "gusher";
    public static final String KEY_MP4FIX = "mp4fix";
    private static final int SELECT_OUTPUT_DIR = 1;
    private static final int SELECT_DOCUMENT_DIR = 2;
    private static final String TAG = "scr_SettingsFragment";
    private Preference donatePreference;
    private Preference noRootModePreference;
    private PreferenceCategory videoCategory;
    private ListPreference videoConfigPreference;
    private ListPreference videoEncoderPreference;
    private ListPreference resolutionPreference;
    private ListPreference orientationPreference;
    private ListPreference timeLapsePreference;
    private ListPreference transformationPreference;
    private ListPreference videoBitratePreference;
    private ListPreference frameRatePreference;
    private CheckBoxPreference verticalFramesPreference;
    private PreferenceCategory audioCategory;
    private ListPreference audioSourcePreference;
    private ListPreference samplingRatePreference;
    private CheckBoxPreference stereoPreference;
    private SliderPreference micGainPreference;
    private PreferenceCategory otherCategory;
    private SliderPreference cameraAlphaPreference;
    private CheckBoxPreference showCameraPreference;
    private CheckBoxPreference hideIconPreference;
    private CheckBoxPreference showTouchesPreference;
    private Preference documentDirPreference;
    private Preference outputDirPreference;
    private CheckBoxPreference stopOnScreenOffPreference;
    private CheckBoxPreference colorFixPreference;
    private Preference gusherPreference;
    private Preference mp4fixPreference;
    private Settings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        String copyrightsStatement = getString(R.string.copyrights_statement, getString(R.string.app_name));
        findPreference(KEY_COPYRIGHTS_STATEMENT).setSummary(copyrightsStatement);

        settings = Settings.getInstance();
        settings.registerOnSharedPreferenceChangeListener(this);

        donatePreference = findPreference(KEY_DONATE);
        donatePreference.setOnPreferenceClickListener(this);

        noRootModePreference = findPreference(KEY_NO_ROOT_MODE);
        noRootModePreference.setOnPreferenceClickListener(this);

        videoCategory = (PreferenceCategory) findPreference(KEY_VIDEO);

        videoConfigPreference = (ListPreference) findPreference(KEY_VIDEO_CONFIG);
        videoConfigPreference.setOnPreferenceChangeListener(this);
        String title = getString(R.string.settings_video_config, Build.MODEL);
        videoConfigPreference.setTitle(title);
        videoConfigPreference.setDialogTitle(title);

        videoEncoderPreference = (ListPreference) findPreference(KEY_VIDEO_ENCODER);
        videoEncoderPreference.setOnPreferenceChangeListener(this);

        resolutionPreference = (ListPreference) findPreference(KEY_RESOLUTION);
        resolutionPreference.setOnPreferenceChangeListener(this);

        orientationPreference = (ListPreference) findPreference(KEY_ORIENTATION);
        orientationPreference.setOnPreferenceChangeListener(this);

        timeLapsePreference = (ListPreference) findPreference(KEY_TIME_LAPSE);
        timeLapsePreference.setOnPreferenceChangeListener(this);

        transformationPreference = (ListPreference) findPreference(KEY_TRANSFORMATION);
        transformationPreference.setOnPreferenceChangeListener(this);

        videoBitratePreference = (ListPreference) findPreference(KEY_VIDEO_BITRATE);
        videoBitratePreference.setOnPreferenceChangeListener(this);

        frameRatePreference = (ListPreference) findPreference(KEY_FRAME_RATE);
        frameRatePreference.setOnPreferenceChangeListener(this);

        verticalFramesPreference = (CheckBoxPreference) findPreference(KEY_VERTICAL_FRAMES);
        verticalFramesPreference.setOnPreferenceChangeListener(this);

        audioCategory = (PreferenceCategory) findPreference(KEY_AUDIO);
        audioSourcePreference = (ListPreference) findPreference(KEY_AUDIO_SOURCE);
        audioSourcePreference.setOnPreferenceChangeListener(this);

        samplingRatePreference = (ListPreference) findPreference(KEY_SAMPLING_RATE);
        samplingRatePreference.setOnPreferenceChangeListener(this);

        stereoPreference = (CheckBoxPreference) findPreference(KEY_STEREO);
        stereoPreference.setOnPreferenceChangeListener(this);

        micGainPreference = (SliderPreference) findPreference(KEY_MIC_GAIN);
        micGainPreference.setOnPreferenceChangeListener(this);

        otherCategory = (PreferenceCategory) findPreference(KEY_OTHER);
        showCameraPreference = (CheckBoxPreference) findPreference(KEY_SHOW_CAMERA);
        showCameraPreference.setOnPreferenceChangeListener(this);

        cameraAlphaPreference = (SliderPreference) findPreference(KEY_CAMERA_ALPHA);
        cameraAlphaPreference.setOnPreferenceChangeListener(this);

        hideIconPreference = (CheckBoxPreference) findPreference(KEY_HIDE_ICON);
        hideIconPreference.setOnPreferenceChangeListener(this);

        showTouchesPreference = (CheckBoxPreference) findPreference(KEY_SHOW_TOUCHES);
        showTouchesPreference.setOnPreferenceChangeListener(this);

        documentDirPreference = findPreference(KEY_DOCUMENT_DIR);
        documentDirPreference.setOnPreferenceClickListener(this);

        outputDirPreference = findPreference(KEY_OUTPUT_DIR);
        outputDirPreference.setOnPreferenceClickListener(this);

        stopOnScreenOffPreference = (CheckBoxPreference) findPreference(KEY_STOP_ON_SCREEN_OFF);
        stopOnScreenOffPreference.setOnPreferenceChangeListener(this);

        colorFixPreference = (CheckBoxPreference) findPreference(KEY_COLOR_FIX);
        colorFixPreference.setOnPreferenceChangeListener(this);

        gusherPreference = findPreference(KEY_GUSHER);
        gusherPreference.setOnPreferenceClickListener(this);

        mp4fixPreference = findPreference(KEY_MP4FIX);
        mp4fixPreference.setOnPreferenceClickListener(this);

        settings.getAudioDriver().addInstallListener(this);
        updateEntries();
        updateValues();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Context context = getActivity();
        addRemovePreference(Utils.hasFrontFacingCamera(context), KEY_SHOW_CAMERA, showCameraPreference, otherCategory);
        addRemovePreference(Utils.hasFrontFacingCamera(context), KEY_CAMERA_ALPHA, cameraAlphaPreference, otherCategory);
    }

    protected void updateValues() {
        updateSelectedVideoConfig();

        videoConfigPreference.setEnabled(settings.isRootEnabled() && settings.getDeviceProfile() != null
                && settings.getDeviceProfile().getVideoConfigs().size() > 0);

        videoEncoderPreference.setValue(String.valueOf(settings.getVideoEncoder()));
        videoEncoderPreference.setSummary(formatVideoEncoderSummary(settings.getVideoEncoder()));

        resolutionPreference.setValue(formatResolutionEntryValue(settings.getResolution()));
        resolutionPreference.setSummary(formatResolutionEntry(settings.getResolution()));

        orientationPreference.setValue(settings.getOrientation().name());
        orientationPreference.setSummary(formatOrientationSummary(settings.getOrientation()));
        orientationPreference.setEnabled(settings.isNoRootVideoEncoder());

        timeLapsePreference.setValue(String.valueOf(settings.getTimeLapse()));
        timeLapsePreference.setSummary(formatTimeLapseSummary(settings.getTimeLapse()));

        transformationPreference.setValue(settings.getTransformation().name());
        transformationPreference.setSummary(formatTransformationSummary(settings.getTransformation()));
        transformationPreference.setEnabled(!settings.isNoRootVideoEncoder() && settings.getVideoEncoder() >= 0);

        videoBitratePreference.setValue(settings.getVideoBitrate().name());
        videoBitratePreference.setSummary(formatVideoBitrateSummary(settings.getVideoBitrate()));

        frameRatePreference.setValue(String.valueOf(settings.getFrameRate()));
        frameRatePreference.setSummary(formatFrameRateSummary(settings.getFrameRate()));
        frameRatePreference.setEnabled(!settings.isNoRootVideoEncoder());

        verticalFramesPreference.setChecked(settings.getVerticalFrames());
        verticalFramesPreference.setEnabled(!settings.isNoRootVideoEncoder());

        audioSourcePreference.setValue(settings.getAudioSource().name());
        audioSourcePreference.setSummary(formatAudioSourceSummary(settings.getAudioSource(), settings.getTimeLapse() != 1));
        audioSourcePreference.setEnabled(settings.getTimeLapse() == 1);

        samplingRatePreference.setValue(settings.getSamplingRate().name());
        samplingRatePreference.setSummary(formatSamplingRateSummary());
        samplingRatePreference.setEnabled(settings.getTimeLapse() == 1 && !settings.getAudioSource().equals(AudioSource.MUTE));

        stereoPreference.setChecked(settings.getStereo());
        stereoPreference.setEnabled(settings.getTimeLapse() == 1 && !settings.getAudioSource().equals(AudioSource.MUTE));

        micGainPreference.setValue(gainToIndex(settings.getMicGain()));
        micGainPreference.setSummary(formatMicGain());
        micGainPreference.setEnabled(settings.getTimeLapse() == 1 && settings.getAudioSource() == AudioSource.MIX);

        hideIconPreference.setChecked(settings.getHideIcon());
        showTouchesPreference.setChecked(settings.getShowTouches());
        showCameraPreference.setChecked(settings.getShowCamera());
        cameraAlphaPreference.setValue((int) (settings.getCameraAlpha() * 100));
        cameraAlphaPreference.setSummary(formatCameraAlphaSummary());
        documentDirPreference.setSummary(settings.getDocumentDirName());
        outputDirPreference.setSummary(settings.getOutputDir().getAbsolutePath());
        stopOnScreenOffPreference.setChecked(settings.getStopOnScreenOff());
        colorFixPreference.setChecked(settings.getColorFix());
        colorFixPreference.setEnabled(!settings.isNoRootVideoEncoder());
    }

    private String formatOrientationSummary(Orientation orientation) {
        if (!settings.isNoRootVideoEncoder()) {
            return getString(R.string.settings_no_root_only);
        }
        switch (orientation) {
            case AUTO:
                return getString(R.string.settings_orientation_auto);
            case LANDSCAPE:
                return getString(R.string.settings_orientation_landscape);
            case PORTRAIT:
                return getString(R.string.settings_orientation_portrait);
        }
        return "";
    }

    private String formatTimeLapseSummary(int timeLapse) {
        if (timeLapse == 1) {
            return getString(R.string.settings_time_lapse_summary_no_time_lapse);
        }
        return getString(R.string.settings_time_lapse_summary, timeLapse);
    }

    private void updateSelectedVideoConfig() {
        if (settings.getDeviceProfile() != null && settings.getDeviceProfile().getVideoConfigs().size() > 0) {
            videoConfigPreference.setValue(getSelectedVideoConfigIndex());
        }
    }

    private String getSelectedVideoConfigIndex() {
        List<VideoConfig> configs = settings.getDeviceProfile().getVideoConfigs();
        for (int i = 0; i < configs.size(); i++) {
            VideoConfig config = configs.get(i);
            if (config.getVideoEncoder() == settings.getVideoEncoder()
                    && config.getResolution() == settings.getResolution()
                    && config.getTransformation() == settings.getTransformation()
                    && config.getVideoBitrate() == settings.getVideoBitrate())
                return String.valueOf(i);
        }
        return null;
    }

    protected void updateEntries() {

        addRemovePreference(settings.isRootFlavor() && !settings.isRootEnabled(), KEY_NO_ROOT_MODE, noRootModePreference, getPreferenceScreen());

        if (addRemovePreference(settings.isRootFlavor(), KEY_VIDEO_CONFIG, videoConfigPreference, videoCategory)) {
            if (settings.getDeviceProfile() != null && settings.getDeviceProfile().getVideoConfigs().size() > 0) {
                videoConfigPreference.setEntries(getVideoConfigEntries());
                videoConfigPreference.setEntryValues(getVideoConfigEntryValues());
                videoConfigPreference.setEnabled(true);
                videoConfigPreference.setSummary(R.string.settings_video_config_summary);
            } else {
                videoConfigPreference.setEnabled(false);
                if (settings.getDeviceProfile() == null) {
                    videoConfigPreference.setSummary(R.string.settings_video_config_summary_no_data);
                } else {
                    videoConfigPreference.setSummary(R.string.settings_video_config_summary_not_available);
                }
            }
        }

        List<Integer> videoEncoders = getVideoEncoders();
        if (addRemovePreference(videoEncoders.size() > 1, KEY_VIDEO_ENCODER, videoEncoderPreference, videoCategory)) {
            videoEncoderPreference.setEntryValues(getVideoEncoderEntryValues(videoEncoders));
            videoEncoderPreference.setEntries(getVideoEncoderEntries(videoEncoders));
        }

        resolutionPreference.setEntryValues(getResolutionEntryValues());
        resolutionPreference.setEntries(getResolutionEntries());

        boolean softwareEncoderOnly = videoEncoders.size() == 1 && videoEncoders.get(0) == -2;
        List<Transformation> transformations = getTransformations();

        if (addRemovePreference(settings.isRootFlavor() && transformations.size() > 1 && !softwareEncoderOnly,
                KEY_TRANSFORMATION, transformationPreference, videoCategory)) {
            transformationPreference.setEntryValues(getTransformationEntryValues(transformations));
            transformationPreference.setEntries(getTransformationEntries(transformations));
        }

        if (addRemovePreference(!settings.isRootFlavor() || settings.getShowAdvanced(), KEY_VIDEO_BITRATE, videoBitratePreference, videoCategory)) {
            ArrayList<VideoBitrate> videoBitrates = getVideoBitrates();
            videoBitratePreference.setEntryValues(getVideoBitrateEntryValues(videoBitrates));
            videoBitratePreference.setEntries(getVideoBitrateEntries(videoBitrates));
        }

        if (addRemovePreference(settings.getShowAdvanced(), KEY_FRAME_RATE, frameRatePreference, videoCategory)) {
            if (Build.VERSION.SDK_INT < 18 || (settings.getDeviceProfile() != null && !settings.getDeviceProfile().isHighEndDevice())) {
                frameRatePreference.setEntryValues(R.array.frame_rate_values_lo_end);
            } else {
                frameRatePreference.setEntryValues(R.array.frame_rate_values);
            }
            frameRatePreference.setEntries(getFrameRateEntries(frameRatePreference.getEntryValues()));
        }

        addRemovePreference(settings.getShowAdvanced(), KEY_VERTICAL_FRAMES, verticalFramesPreference, videoCategory);

        CharSequence[] mainEntries = getResources().getStringArray(settings.isRootEnabled() ? R.array.audio_source_entries : R.array.audio_source_entries_no_root);
        CharSequence[] entries = new CharSequence[mainEntries.length];
        CharSequence[] entryValues = getResources().getStringArray(settings.isRootEnabled() ? R.array.audio_source_values : R.array.audio_source_values_no_root);
        for (int i = 0; i < mainEntries.length; i++) {
            AudioSource audioSource = AudioSource.valueOf(entryValues[i].toString());
            if (audioSource.getRequiresDriver()) {
                if (settings.getDeviceProfile() != null && !settings.getDeviceProfile().isInternalAudioStable()) {
                    entries[i] = getTwoLineEntry(mainEntries[i], getString(R.string.settings_audio_incompatible, Build.VERSION.RELEASE));
                } else {
                    entries[i] = getTwoLineEntry(mainEntries[i], audioSource == AudioSource.MIX ?
                                    getString(R.string.settings_audio_use_headphones) :
                                    getString(R.string.settings_audio_experimental)
                    );
                }
            } else {
                entries[i] = mainEntries[i];
            }
        }
        audioSourcePreference.setEntries(entries);
        audioSourcePreference.setEntryValues(entryValues);

        updateSamplingRate();

        addRemovePreference(settings.isRootFlavor(), KEY_COLOR_FIX, colorFixPreference, otherCategory);

        String[] timeLapseValues = getResources().getStringArray(R.array.time_lapse_values);
        timeLapsePreference.setEntryValues(timeLapseValues);
        timeLapsePreference.setEntries(getTimeLapseEntries(timeLapseValues));

        addRemovePreference(settings.isRootFlavor(), KEY_MIC_GAIN, micGainPreference, audioCategory);

        addRemovePreference(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP, KEY_DOCUMENT_DIR, documentDirPreference, otherCategory);
    }

    private void updateSamplingRate() {
        AudioSource audioSource = Settings.getInstance().getAudioSource();
        if (audioSource.getRequiresDriver()) {
            int outSamplingRate = Settings.getInstance().getAudioDriver().getSamplingRate();
            ArrayList<SamplingRate> samplingRates = new ArrayList<SamplingRate>(3);
            for (SamplingRate samplingRate : SamplingRate.values()) {
                if ((outSamplingRate % samplingRate.getSamplingRate()) == 0
                        && outSamplingRate / samplingRate.getSamplingRate() <= 4) {
                    samplingRates.add(samplingRate);
                }
            }
            samplingRatePreference.setEntryValues(getSamplingRateEntryValues(samplingRates));
            samplingRatePreference.setEntries(getSamplingRateEntries(samplingRates));
        } else {
            samplingRatePreference.setEntryValues(getSamplingRateEntryValues(SamplingRate.STANDARD));
            samplingRatePreference.setEntries(getSamplingRateEntries(SamplingRate.STANDARD));
        }
    }

    private CharSequence[] getSamplingRateEntryValues(List<SamplingRate> samplingRates) {
        String[] entryValues = new String[samplingRates.size()];
        for (int i = 0; i < entryValues.length; i++) {
            entryValues[i] = samplingRates.get(i).name();
        }
        return entryValues;
    }

    private CharSequence[] getSamplingRateEntries(List<SamplingRate> samplingRates) {
        String[] entries = new String[samplingRates.size()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = samplingRates.get(i).getLabel();
        }
        return entries;
    }

    private CharSequence getTwoLineEntry(CharSequence firstLine, CharSequence secondLine) {
        return Html.fromHtml(
                firstLine + "<br/><small><font color=\"@android:secondary_text_dark\">" + secondLine + "</font></small>"
        );
    }

    private CharSequence[] getVideoConfigEntries() {
        List<VideoConfig> configs = settings.getDeviceProfile().getVideoConfigs();
        CharSequence[] entries = new CharSequence[configs.size()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = formatVideoConfig(configs.get(i));
        }
        return entries;
    }

    private CharSequence formatVideoConfig(VideoConfig videoConfig) {
        return getTwoLineEntry(
                getString(R.string.settings_video_config_entry, videoConfig.getResolution().getWidth(), videoConfig.getResolution().getHeight(), videoConfig.getFrameRate()),
                formatVideoEncoderEntry(videoConfig.getVideoEncoder()) +
                        (videoConfig.getVideoEncoder() == VideoEncoder.FFMPEG_MPEG_4  ? "" : "&emsp;" + formatTransformationEntry(videoConfig.getTransformation())) +
                        "&emsp;" + getString(R.string.settings_video_config_entry_stability, videoConfig.getStability())
        );
    }

    private String[] getVideoConfigEntryValues() {
        String[] indexes = new String[settings.getDeviceProfile().getVideoConfigs().size()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = String.valueOf(i);
        }
        return indexes;
    }

    private boolean addRemovePreference(boolean add, String key, Preference preference, PreferenceGroup category) {
        if (!add && category.findPreference(key) != null) {
            category.removePreference(preference);
        } else if (add && category.findPreference(key) == null) {
            category.addPreference(preference);
        }
        return add;
    }

    private List<Integer> getVideoEncoders() {
        if (!settings.getShowUnstable() && settings.getDeviceProfile() != null)
            return settings.getDeviceProfile().getStableVideoEncoders();

        return Arrays.asList(VideoEncoder.getAllSupportedEncoders(!settings.isRootEnabled()));
    }

    private String[] getVideoEncoderEntryValues(List<Integer> videoEncoders) {
        String[] entryValues = new String[videoEncoders.size()];
        for (int i = 0; i < videoEncoders.size(); i++) {
            entryValues[i] = String.valueOf(videoEncoders.get(i));
        }
        return entryValues;
    }

    private String[] getVideoEncoderEntries(List<Integer> entryValues) {
        String[] entries = new String[entryValues.size()];
        for (int i = 0; i < entryValues.size(); i++) {
            entries[i] = formatVideoEncoderEntry(entryValues.get(i));
        }
        return entries;
    }

    private String formatVideoEncoderEntry(int encoder) {
        String entry = null;
        switch (encoder) {
            case VideoEncoder.FFMPEG_MPEG_4:
                entry = getString(R.string.settings_video_encoder_ffmpeg_mpeg_4);
                break;
            case VideoEncoder.H264:
                entry = getString(R.string.settings_video_encoder_h264);
                break;
            case VideoEncoder.MPEG_4_SP:
                entry = getString(R.string.settings_video_encoder_mpeg_4_sp);
                break;
            case VideoEncoder.NO_ROOT_H264:
                entry = getString(settings.isRootFlavor() ?
                        R.string.settings_video_encoder_no_root_h264
                        : R.string.settings_video_encoder_h264);
                break;
            case VideoEncoder.NO_ROOT_MPEG_4:
                entry = getString(settings.isRootFlavor() ?
                        R.string.settings_video_encoder_no_root_mpeg_4
                        : R.string.settings_video_encoder_mpeg_4);
                break;
        }
        if (settings.getDeviceProfile() != null && settings.getDeviceProfile().hideVideoEncoder(encoder)) {
            entry = getString(R.string.settings_unstable, entry);
        }
        return entry;
    }

    private CharSequence[] getResolutionEntries() {
        Resolution[] resolutions = settings.getResolutions();
        ArrayList<String> entries = new ArrayList<String>(resolutions.length);
        for (Resolution resolution : resolutions) {
            if (!settings.getShowUnstable() && settings.getDeviceProfile() != null
                    && settings.getDeviceProfile().hideResolution(resolution))
                continue;
            entries.add(formatResolutionEntry(resolution));
        }
        return entries.toArray(new String[entries.size()]);
    }

    private String formatResolutionEntry(Resolution r) {
        String entry = String.format(getString(r.getLabelId(), r.getWidth(), r.getHeight()));
        if (settings.getDeviceProfile() != null && settings.getDeviceProfile().hideResolution(r)) {
            entry = getString(R.string.settings_unstable, entry);
        }
        return entry;
    }

    private CharSequence[] getResolutionEntryValues() {
        Resolution[] resolutions = settings.getResolutions();
        ArrayList<String> values = new ArrayList<String>(resolutions.length);
        for (Resolution resolution : resolutions) {
            if (!settings.getShowUnstable() && settings.getDeviceProfile() != null
                    && settings.getDeviceProfile().hideResolution(resolution))
                continue;
            values.add(formatResolutionEntryValue(resolution));
        }
        return values.toArray(new String[values.size()]);
    }

    private String formatResolutionEntryValue(Resolution r) {
        return r.getWidth() + "x" + r.getHeight();
    }

    private Resolution findResolution(String resolution) {
        if (resolution != null) {
            String[] widthAndHeight = resolution.split("x");
            if (widthAndHeight.length == 2) {
                int width = Integer.parseInt(widthAndHeight[0]);
                int height = Integer.parseInt(widthAndHeight[1]);
                for (Resolution r : settings.getResolutions()) {
                    if (r.getWidth() == width && r.getHeight() == height) {
                        return r;
                    }
                }
            }
        }
        Log.w(TAG, "Resolution ont found " + resolution);
        return settings.getDefaultResolution();
    }

    private List<Transformation> getTransformations() {
        if (!settings.getShowUnstable() && settings.getDeviceProfile() != null)
            return settings.getDeviceProfile().getStableTransformations();

        Transformation[] allTransformations = Build.VERSION.SDK_INT < 18 ?
                new Transformation[]{Transformation.CPU, Transformation.GPU}
                : new Transformation[]{Transformation.CPU, Transformation.GPU, Transformation.OES};

        return Arrays.asList(allTransformations);
    }

    private String[] getTransformationEntryValues(List<Transformation> transformations) {
        String[] entryValues = new String[transformations.size()];
        for (int i = 0; i < transformations.size(); i++) {
            entryValues[i] = transformations.get(i).name();
        }
        return entryValues;
    }

    private String[] getTransformationEntries(List<Transformation> transformations) {
        String[] entries = new String[transformations.size()];
        for (int i = 0; i < transformations.size(); i++) {
            entries[i] = formatTransformationEntry(transformations.get(i));
        }
        return entries;
    }

    private String formatTransformationEntry(Transformation transformation) {
        String entry = null;
        switch (transformation) {
            case CPU:
                entry = getString(R.string.settings_transformation_cpu);
                break;
            case GPU:
                entry = getString(R.string.settings_transformation_gpu);
                break;
            case OES:
                entry = getString(R.string.settings_transformation_oes);
                break;
        }
        if (settings.getDeviceProfile() != null && settings.getDeviceProfile().hideTransformation(transformation)) {
            entry = getString(R.string.settings_unstable, entry);
        }
        return entry;
    }

    private ArrayList<VideoBitrate> getVideoBitrates() {
        ArrayList<VideoBitrate> bitrates = new ArrayList<VideoBitrate>(VideoBitrate.values().length);
        for (VideoBitrate bitrate : VideoBitrate.values()) {
            if (!settings.getShowUnstable() && settings.getDeviceProfile() != null
                    && settings.getDeviceProfile().hideVideoBitrate(bitrate))
                continue;
            if (!settings.isRootFlavor() && (bitrate == VideoBitrate.BITRATE_AUTO || bitrate == VideoBitrate.BITRATE_30_MBPS))
                continue;
            bitrates.add(bitrate);
        }
        return bitrates;
    }

    private String[] getVideoBitrateEntryValues(ArrayList<VideoBitrate> bitrates) {
        String[] entryValues = new String[bitrates.size()];
        for (int i = 0; i < bitrates.size(); i++) {
            entryValues[i] = bitrates.get(i).name();
        }
        return entryValues;
    }

    private String[] getVideoBitrateEntries(ArrayList<VideoBitrate> bitrates) {
        String[] entries = new String[bitrates.size()];
        for (int i = 0; i < bitrates.size(); i++) {
            entries[i] = bitrates.get(i).getLabel();
            if (settings.getDeviceProfile() != null && settings.getDeviceProfile().hideVideoBitrate(bitrates.get(i))) {
                entries[i] = getString(R.string.settings_unstable, entries[i]);
            }
        }
        return entries;
    }

    private String[] getTimeLapseEntries(String[] values) {
        String[] entries = new String[values.length];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = formatTimeLapseEntry(Integer.parseInt(values[i]));
        }
        return entries;
    }

    private String formatTimeLapseEntry(int timeLapse) {
        if (timeLapse == 1) {
            return getString(R.string.settings_time_lapse_entry_no_time_lapse, timeLapse);
        }
        return getString(R.string.settings_time_lapse_entry, timeLapse);
    }

    private String formatVideoEncoderSummary(int videoEncoder) {
        switch (videoEncoder) {
            case VideoEncoder.FFMPEG_MPEG_4:
                return getString(R.string.settings_video_encoder_ffmpeg_summary);
            case VideoEncoder.H264:
                return getString(R.string.settings_video_encoder_built_in_summary,
                        getString(R.string.settings_video_encoder_h264));
            case VideoEncoder.MPEG_4_SP:
                return getString(R.string.settings_video_encoder_built_in_summary,
                        getString(R.string.settings_video_encoder_mpeg_4_sp));
            case VideoEncoder.NO_ROOT_H264:
                return getString(R.string.settings_video_encoder_built_in_summary, getString(
                                settings.isRootFlavor() ?
                                        R.string.settings_video_encoder_no_root_h264
                                        : R.string.settings_video_encoder_h264)
                );
            case VideoEncoder.NO_ROOT_MPEG_4:
                return getString(R.string.settings_video_encoder_built_in_summary, getString(
                        settings.isRootFlavor() ?
                                R.string.settings_video_encoder_no_root_mpeg_4
                                : R.string.settings_video_encoder_mpeg_4));
        }
        return "";
    }

    private String formatTransformationSummary(Transformation transformation) {
        if (VideoEncoder.isSoftware(settings.getVideoEncoder())) {
            return getString(R.string.settings_transformation_sw_summary);
        }
        if (settings.isNoRootVideoEncoder()) {
            return getString(R.string.settings_root_only);
        }
        switch (transformation) {
            case CPU:
                return getString(R.string.settings_transformation_cpu_summary);
            case GPU:
                return getString(R.string.settings_transformation_gpu_summary,
                        getString(R.string.settings_transformation_gpu));
            case OES:
                return getString(R.string.settings_transformation_gpu_summary,
                        getString(R.string.settings_transformation_oes));
        }
        return "";
    }

    private String formatVideoBitrateSummary(VideoBitrate bitrate) {
        return String.format(getString(R.string.settings_video_bitrate_summary), bitrate.getLabel());
    }

    private CharSequence[] getFrameRateEntries(CharSequence[] values) {
        String[] entries = new String[values.length];
        for (int i = 0; i < entries.length; i++) {
            int frameRate = Integer.parseInt(values[i].toString());
            if (frameRate == -1) {
                entries[i] = getString(R.string.settings_frame_rate_max);
            } else {
                entries[i] = String.format(getString(R.string.settings_frame_rate_up_to), frameRate);
            }
        }
        return entries;
    }

    private String formatFrameRateSummary(int frameRate) {
        if (settings.isNoRootVideoEncoder() || frameRate == -1) {
            return getString(R.string.settings_frame_rate_max_summary);
        }
        return String.format(getString(R.string.settings_frame_rate_summary), frameRate);
    }

    private String formatAudioSourceSummary(AudioSource source, boolean isTimeLapse) {
        if (isTimeLapse) {
            return getString(R.string.settings_audio_time_lapse_summary);
        }
        switch (source) {
            case MIC:
                return getString(R.string.settings_audio_mic_summary);
            case MUTE:
                return getString(R.string.settings_audio_mute_summary);
            case INTERNAL:
                return getString(R.string.settings_audio_internal_summary);
            case MIX:
                return getString(R.string.settings_audio_mix_summary);
        }
        return "";
    }

    private String formatSamplingRateSummary() {
        return settings.getSamplingRate().getLabel();
    }

    private int gainToIndex(int gain) {
        for (int i = 0; i <= 4; i++){
            if (gain == indexToGain(i)) {
                return i;
            }
        }
        return 0;
    }

    private int indexToGain(int index) {
        return  (int) Math.pow(2, index);
    }

    private int gainToDecibel(int gain) {
        return (int) (20.0 * Math.log10(gain));
    }

    private String formatMicGain() {
        if (settings.getAudioSource() != AudioSource.MIX) {
            return getString(R.string.settings_mic_gain_summary_mix_only, getString(R.string.settings_audio_mix));
        }
        if (settings.getMicGain() == 1) {
            return getString(R.string.settings_mic_gain_summary_default);
        }
        return getString(R.string.settings_mic_gain_summary, gainToDecibel(settings.getMicGain()));
    }

    private String formatCameraAlphaSummary() {
        if (getActivity() == null) return "";
        if (settings.getCameraAlpha() == 1.0f) {
            return getString(R.string.settings_camera_alpha_summary_100);
        }
        int percentage = (int) (settings.getCameraAlpha() * 100);
        return getString(R.string.settings_camera_alpha_summary, percentage);
    }

    private void openOutputDirChooser() {
        Intent intent = new Intent(getActivity(), DirectoryChooserActivity.class);
        intent.setData(Uri.fromFile(Settings.getInstance().getOutputDir()));
        intent.putExtra(DirectoryChooserActivity.DEFAULT_DIR_EXTRA, Settings.getInstance().getDefaultOutputDir().getAbsolutePath());
        startActivityForResult(intent, SELECT_OUTPUT_DIR);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String valueString = null;
        Boolean selected = Boolean.FALSE;
        if (preference instanceof ListPreference) {
            valueString = (String) newValue;
        }
        if (preference instanceof CheckBoxPreference) {
            selected = (Boolean) newValue;
        }

        if (preference == videoConfigPreference) {
            int configIndex = Integer.parseInt(valueString);
            VideoConfig videoConfig = settings.getDeviceProfile().getVideoConfigs().get(configIndex);
            settings.setVideoEncoder(videoConfig.getVideoEncoder());
            settings.setResolution(videoConfig.getResolution());
            settings.setTransformation(videoConfig.getTransformation());
            settings.setVideoBitrate(videoConfig.getVideoBitrate());
            updateValues();

        } else if (preference == videoEncoderPreference) {
            int videoEncoder = Integer.parseInt(valueString);
            settings.setVideoEncoder(videoEncoder);
            updateValues();

        } else if (preference == resolutionPreference) {
            Resolution resolution = findResolution(valueString);
            settings.setResolution(resolution);
            preference.setSummary(formatResolutionEntry(resolution));
            updateSelectedVideoConfig();

        } else if (preference == orientationPreference) {
            Orientation orientation = Orientation.valueOf(valueString);
            settings.setOrientation(orientation);
            preference.setSummary(formatOrientationSummary(orientation));

        } else if (preference == timeLapsePreference) {
            settings.setTimeLapse(Integer.parseInt(valueString));
            updateValues();

        } else if (preference == transformationPreference) {
            Transformation transformation = Transformation.valueOf(valueString);
            settings.setTransformation(transformation);
            preference.setSummary(formatTransformationSummary(transformation));
            updateSelectedVideoConfig();

        } else if (preference == videoBitratePreference) {
            VideoBitrate bitrate = VideoBitrate.valueOf(valueString);
            settings.setVideoBitrate(bitrate);
            preference.setSummary(formatVideoBitrateSummary(bitrate));
            updateSelectedVideoConfig();

        } else if (preference == frameRatePreference) {
            int frameRate = Integer.parseInt(valueString);
            settings.setFrameRate(frameRate);
            preference.setSummary(formatFrameRateSummary(frameRate));

        } else if (preference == verticalFramesPreference) {
            settings.setVerticalFrames(selected);
        } else if (preference == audioSourcePreference) {
            AudioSource source = AudioSource.valueOf(valueString);
            settings.setAudioSource(source);
            AudioDriver audioDriver = settings.getAudioDriver();
            if (source.getRequiresDriver() && audioDriver.shouldInstall()) {
                if (audioDriver.getRequiresHardInstall() && !settings.getDisableAudioWarning()) {
                    new AudioWarningDialogFragment().show(getFragmentManager(), AudioWarningDialogFragment.FRAGMENT_TAG);
                } else {
                    audioDriver.install();
                }
            }
        } else if (preference == samplingRatePreference) {
            SamplingRate rate = SamplingRate.valueOf(valueString);
            settings.setSamplingRate(rate);
            preference.setSummary(rate.getLabel());
        } else if (preference == stereoPreference) {
            settings.setStereo(selected);
        } else if (preference == micGainPreference) {
            settings.setMicGain(indexToGain((Integer) newValue));
            preference.setSummary(formatMicGain());
        } else if (preference == hideIconPreference) {
            settings.setHideIcon(selected);
        } else if (preference == showTouchesPreference) {
            settings.setShowTouches(selected);
        } else if (preference == showCameraPreference) {
            settings.setShowCamera(selected);
        } else if (preference == cameraAlphaPreference) {
            settings.setCameraAlpha(((Integer) newValue) / 100.0f);
            cameraAlphaPreference.setSummary(formatCameraAlphaSummary());
        } else if (preference == stopOnScreenOffPreference) {
            settings.setStopOnScreenOff(selected);
        } else if (preference == colorFixPreference) {
            settings.setColorFix(selected);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == documentDirPreference) {
            openDocumentPicker();
            return true;
        } else if (preference == outputDirPreference) {
            openOutputDirChooser();
            return true;
        }  else if (preference == donatePreference) {
            donate();
            return true;
        } else if (preference == noRootModePreference) {
            enableRoot();
            return true;
        } else if (preference == gusherPreference) {
            openRecommendedApp(GusherDialogFragment.GUSHER_PACKAGE_ID);
            return true;
        } else if (preference == mp4fixPreference) {
            openRecommendedApp(RecorderService.VIDEO_REPAIR_PACKAGE);
            return true;
        }
        return false;
    }

    private void openRecommendedApp(String packageId) {
        Activity activity = getActivity();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageId + "&referrer=utm_source%3DSCR%26utm_medium%3Dsettings%26utm_campaign%3Drecommended_apps"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.rating_play_error, Toast.LENGTH_LONG).show();
        }
    }

    private void donate() {
        Activity activity = getActivity();
        if (activity != null) {
            Intent payPalIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=3TDE5GYWQYVL6"));
            try {
                activity.startActivity(payPalIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting donation intent", e);
            }
        }
    }

    private void enableRoot() {
        settings.setRootEnabled(true);
        Activity activity = getActivity();
        if (activity != null) {
            Intent serviceIntent = new Intent(activity, RecorderService.class);
            serviceIntent.setAction(RecorderService.ENABLE_ROOT_ACTION);
            activity.startService(serviceIntent);
        }
    }

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, SELECT_DOCUMENT_DIR);
    }

    private void setDocumentDir(Intent data) {
        Uri treeUri = data.getData();
        DocumentFile documentDir = DocumentFile.fromTreeUri(getActivity(), treeUri);

        // Persist access permissions.
        getActivity().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        Settings.getInstance().setDocumentDirUri(treeUri);
        Settings.getInstance().setDocumentDirName(documentDir.getName());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_OUTPUT_DIR) {
            if (resultCode == Activity.RESULT_OK) {
                settings.setOutputDir(new File(data.getData().getPath()));
                outputDirPreference.setSummary(settings.getOutputDir().getAbsolutePath());
            }
        } else if (requestCode == SELECT_DOCUMENT_DIR) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                setDocumentDir(data);
                documentDirPreference.setSummary(settings.getDocumentDirName());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        settings.unregisterOnSharedPreferenceChangeListener(this);
        settings.getAudioDriver().removeInstallListener(this);
    }

    @Override
    public void onInstall(InstallationStatus status) {
        updateValues();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.ROOT_ENABLED.equals(key)) {
            updateEntries();
            updateValues();
        } else if (Settings.AUDIO_SOURCE.equals(key)) {
            updateSamplingRate();
            updateValues();
        }
    }
}
