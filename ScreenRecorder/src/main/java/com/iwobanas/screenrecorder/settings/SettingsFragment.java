package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.text.Html;
import android.util.Log;

import com.iwobanas.screenrecorder.DirectoryChooserActivity;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.audio.AudioDriver;
import com.iwobanas.screenrecorder.audio.InstallationStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, AudioDriver.OnInstallListener {
    public static final String KEY_COPYRIGHTS_STATEMENT = "copyrights_statement";
    public static final String KEY_VIDEO = "video";
    public static final String KEY_VIDEO_CONFIG = "video_config";
    public static final String KEY_VIDEO_ENCODER = "video_encoder";
    public static final String KEY_RESOLUTION = "resolution";
    public static final String KEY_TRANSFORMATION = "transformation";
    public static final String KEY_VIDEO_BITRATE = "video_bitrate";
    public static final String KEY_FRAME_RATE = "frame_rate";
    public static final String KEY_VERTICAL_FRAMES = "vertical_frames";
    public static final String KEY_AUDIO_SOURCE = "audio_source";
    public static final String KEY_SAMPLING_RATE = "sampling_rate";
    public static final String KEY_HIDE_ICON = "hide_icon";
    public static final String KEY_SHOW_TOUCHES = "show_touches";
    public static final String KEY_OUTPUT_DIR = "output_dir";
    public static final String KEY_STOP_ON_SCREEN_OFF = "stop_on_screen_off";
    public static final String KEY_COLOR_FIX = "color_fix";
    private static final int SELECT_OUTPUT_DIR = 1;
    private static final String TAG = "scr_SettingsFragment";
    private PreferenceCategory videoCategory;
    private ListPreference videoConfigPreference;
    private ListPreference videoEncoderPreference;
    private ListPreference resolutionPreference;
    private ListPreference transformationPreference;
    private ListPreference videoBitratePreference;
    private ListPreference frameRatePreference;
    private CheckBoxPreference verticalFramesPreference;
    private ListPreference audioSourcePreference;
    private ListPreference samplingRatePreference;
    private CheckBoxPreference hideIconPreference;
    private CheckBoxPreference showTouchesPreference;
    private Preference outputDirPreference;
    private CheckBoxPreference stopOnScreenOffPreference;
    private CheckBoxPreference colorFixPreference;
    private Settings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        String copyrightsStatement = getString(R.string.copyrights_statement, getString(R.string.app_name));
        findPreference(KEY_COPYRIGHTS_STATEMENT).setSummary(copyrightsStatement);

        settings = Settings.getInstance();

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

        transformationPreference = (ListPreference) findPreference(KEY_TRANSFORMATION);
        transformationPreference.setOnPreferenceChangeListener(this);

        videoBitratePreference = (ListPreference) findPreference(KEY_VIDEO_BITRATE);
        videoBitratePreference.setOnPreferenceChangeListener(this);

        frameRatePreference = (ListPreference) findPreference(KEY_FRAME_RATE);
        frameRatePreference.setOnPreferenceChangeListener(this);

        verticalFramesPreference = (CheckBoxPreference) findPreference(KEY_VERTICAL_FRAMES);
        verticalFramesPreference.setOnPreferenceChangeListener(this);

        audioSourcePreference = (ListPreference) findPreference(KEY_AUDIO_SOURCE);
        audioSourcePreference.setOnPreferenceChangeListener(this);

        samplingRatePreference = (ListPreference) findPreference(KEY_SAMPLING_RATE);
        samplingRatePreference.setOnPreferenceChangeListener(this);

        hideIconPreference = (CheckBoxPreference) findPreference(KEY_HIDE_ICON);
        hideIconPreference.setOnPreferenceChangeListener(this);

        showTouchesPreference = (CheckBoxPreference) findPreference(KEY_SHOW_TOUCHES);
        showTouchesPreference.setOnPreferenceChangeListener(this);

        outputDirPreference = findPreference(KEY_OUTPUT_DIR);
        outputDirPreference.setOnPreferenceClickListener(this);

        stopOnScreenOffPreference = (CheckBoxPreference) findPreference(KEY_STOP_ON_SCREEN_OFF);
        stopOnScreenOffPreference.setOnPreferenceChangeListener(this);

        colorFixPreference = (CheckBoxPreference) findPreference(KEY_COLOR_FIX);
        colorFixPreference.setOnPreferenceChangeListener(this);

        settings.getAudioDriver().addInstallListener(this);
        updateEntries();
        updateValues();
    }

    protected void updateValues() {
        updateSelectedVideoConfig();

        videoEncoderPreference.setValue(String.valueOf(settings.getVideoEncoder()));
        videoEncoderPreference.setSummary(formatVideoEncoderSummary(settings.getVideoEncoder()));

        resolutionPreference.setValue(formatResolutionEntryValue(settings.getResolution()));
        resolutionPreference.setSummary(formatResolutionEntry(settings.getResolution()));

        transformationPreference.setValue(settings.getTransformation().name());
        transformationPreference.setSummary(formatTransformationSummary(settings.getTransformation()));
        transformationPreference.setEnabled(settings.getVideoEncoder() >= 0);

        videoBitratePreference.setValue(settings.getVideoBitrate().name());
        videoBitratePreference.setSummary(formatVideoBitrateSummary(settings.getVideoBitrate()));

        frameRatePreference.setValue(String.valueOf(settings.getFrameRate()));
        frameRatePreference.setSummary(formatFrameRateSummary(settings.getFrameRate()));

        verticalFramesPreference.setChecked(settings.getVerticalFrames());

        audioSourcePreference.setValue(settings.getAudioSource().name());
        audioSourcePreference.setSummary(formatAudioSourceSummary(settings.getAudioSource()));

        samplingRatePreference.setValue(settings.getSamplingRate().name());
        samplingRatePreference.setSummary(formatSamplingRateSummary());
        samplingRatePreference.setEnabled(settings.getAudioSource().equals(AudioSource.MIC));

        hideIconPreference.setChecked(settings.getHideIcon());
        showTouchesPreference.setChecked(settings.getShowTouches());
        outputDirPreference.setSummary(settings.getOutputDir().getAbsolutePath());
        stopOnScreenOffPreference.setChecked(settings.getStopOnScreenOff());
        colorFixPreference.setChecked(settings.getColorFix());
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

        if (settings.getDeviceProfile() != null && settings.getDeviceProfile().getVideoConfigs().size() > 0) {
            videoConfigPreference.setEntries(getVideoConfigEntries());
            videoConfigPreference.setEntryValues(getVideoConfigEntryValues());
            videoConfigPreference.setEnabled(true);
            videoConfigPreference.setSummary(R.string.settings_video_config_summary);
        } else {
            videoConfigPreference.setEnabled(false);
            videoConfigPreference.setSummary(R.string.settings_video_config_summary_no_data);
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

        if (addRemovePreference(transformations.size() > 1 && !softwareEncoderOnly,
                KEY_TRANSFORMATION, transformationPreference, videoCategory)) {
            transformationPreference.setEntryValues(getTransformationEntryValues(transformations));
            transformationPreference.setEntries(getTransformationEntries(transformations));
        }

        if (addRemovePreference(settings.getShowAdvanced(), KEY_VIDEO_BITRATE, videoBitratePreference, videoCategory)) {
            ArrayList<VideoBitrate> videoBitrates = getVideoBitrates();
            videoBitratePreference.setEntryValues(getVideoBitrateEntryValues(videoBitrates));
            videoBitratePreference.setEntries(getVideoBitrateEntries(videoBitrates));
        }

        if (addRemovePreference(settings.getShowAdvanced(), KEY_FRAME_RATE, frameRatePreference, videoCategory)) {
            if (Build.VERSION.SDK_INT < 18 || (settings.getDeviceProfile() != null && !settings.getDeviceProfile().isHighEndDevice())) {
                frameRatePreference.setEntryValues(R.array.frame_rate_values_lo_end);
            }
            frameRatePreference.setEntries(getFrameRateEntries(frameRatePreference.getEntryValues()));
        }

        addRemovePreference(settings.getShowAdvanced(), KEY_VERTICAL_FRAMES, verticalFramesPreference, videoCategory);

        if (Build.VERSION.SDK_INT == 17) {
            CharSequence[] entries = audioSourcePreference.getEntries();
            entries[2] = Html.fromHtml(getString(R.string.settings_audio_internal_experimental) +
                            "<br/><small><font color=\"@android:secondary_text_dark\">" +
                            getString(R.string.settings_audio_internal_not_supported, Build.VERSION.RELEASE) +
                            "</font></small>"
            );
            audioSourcePreference.setEntries(entries);
        } else if (settings.getDeviceProfile() != null && !settings.getDeviceProfile().isInternalAudioStable()) {
            CharSequence[] entries = audioSourcePreference.getEntries();
            entries[2] = Html.fromHtml(getString(R.string.settings_audio_internal_experimental) +
                            "<br/><small><font color=\"@android:secondary_text_dark\">" +
                            getString(R.string.settings_audio_internal_incompatible) +
                            "</font></small>"
            );
            audioSourcePreference.setEntries(entries);
        }
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
        return Html.fromHtml(
                getString(R.string.settings_video_config_entry, videoConfig.getResolution().getWidth(), videoConfig.getResolution().getHeight(), videoConfig.getFrameRate()) +
                        "<br/><small><font color=\"@android:secondary_text_dark\">" +
                        formatVideoEncoderEntry(videoConfig.getVideoEncoder()) +
                        (videoConfig.getVideoEncoder() == Settings.FFMPEG_MPEG_4_ENCODER ? "" : "&emsp;" + formatTransformationEntry(videoConfig.getTransformation())) +
                        "&emsp;" + getString(R.string.settings_video_config_entry_stability, videoConfig.getStability()) +"</font></small>"
        );
    }

    private String[] getVideoConfigEntryValues() {
        String[] indexes = new String[settings.getDeviceProfile().getVideoConfigs().size()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = String.valueOf(i);
        }
        return indexes;
    }

    private boolean addRemovePreference(boolean add, String key, Preference preference, PreferenceCategory category) {
        if (!add && category.findPreference(key) != null) {
            category.removePreference(preference);
        } else if (category.findPreference(key) == null) {
            category.addPreference(preference);
        }
        return add;
    }

    private List<Integer> getVideoEncoders() {
        if (!settings.getShowUnstable() && settings.getDeviceProfile() != null)
            return settings.getDeviceProfile().getStableVideoEncoders();

        Integer[] allEncoders = Utils.isX86() ?
                new Integer[]{MediaRecorder.VideoEncoder.H264, MediaRecorder.VideoEncoder.MPEG_4_SP}
                : new Integer[]{MediaRecorder.VideoEncoder.H264, Settings.FFMPEG_MPEG_4_ENCODER, MediaRecorder.VideoEncoder.MPEG_4_SP};

        return Arrays.asList(allEncoders);
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
            case -2:
                entry = getString(R.string.settings_video_encoder_ffmpeg_mpeg_4);
                break;
            case 2:
                entry = getString(R.string.settings_video_encoder_h264);
                break;
            case 3:
                entry = getString(R.string.settings_video_encoder_mpeg_4_sp);
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

    private String formatVideoEncoderSummary(int videoEncoder) {
        switch (videoEncoder) {
            case Settings.FFMPEG_MPEG_4_ENCODER:
                return getString(R.string.settings_video_encoder_ffmpeg_summary);
            case MediaRecorder.VideoEncoder.H264:
                return String.format(getString(
                                R.string.settings_video_encoder_built_in_summary),
                        getString(R.string.settings_video_encoder_h264)
                );
            case MediaRecorder.VideoEncoder.MPEG_4_SP:
                return String.format(getString(
                                R.string.settings_video_encoder_built_in_summary),
                        getString(R.string.settings_video_encoder_mpeg_4_sp)
                );
        }
        return "";
    }

    private String formatTransformationSummary(Transformation transformation) {
        if (settings.getVideoEncoder() < 0) {
            return getString(R.string.settings_transformation_sw_summary);
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
        if (frameRate == -1) {
            return getString(R.string.settings_frame_rate_max_summary);
        }
        return String.format(getString(R.string.settings_frame_rate_summary), frameRate);
    }

    private String formatAudioSourceSummary(AudioSource source) {
        switch (source) {
            case MIC:
                return getString(R.string.settings_audio_mic_summary);
            case MUTE:
                return getString(R.string.settings_audio_mute_summary);
            case INTERNAL:
                return getString(R.string.settings_audio_internal_summary);
        }
        return "";
    }

    private String formatSamplingRateSummary() {
        if (settings.getAudioSource().equals(AudioSource.INTERNAL)) {
            int rate = settings.getAudioDriver().getSamplingRate();
            for (SamplingRate r : SamplingRate.values()) {
                if (r.getCommand().equals(String.valueOf(rate))) {
                    return r.getLabel();
                }
            }
            return String.valueOf(rate / 1000) + "kHz";
        }
        return settings.getSamplingRate().getLabel();
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
            if (Build.VERSION.SDK_INT != 17) {
                AudioSource source = AudioSource.valueOf(valueString);
                settings.setAudioSource(source);
                updateValues();
            } else {
                return false;
            }
        } else if (preference == samplingRatePreference) {
            SamplingRate rate = SamplingRate.valueOf(valueString);
            settings.setSamplingRate(rate);
            preference.setSummary(rate.getLabel());
        } else if (preference == hideIconPreference) {
            if (getResources().getBoolean(R.bool.taniosc)) {
                try {
                    new HideIconDialogFragment().show(getFragmentManager(), "hideWatermark");
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Couldn't display dialog fragment. Is it already added?", e);
                }
                return false;
            } else {
                settings.setHideIcon(selected);
            }
        } else if (preference == showTouchesPreference) {
            settings.setShowTouches(selected);
        } else if (preference == stopOnScreenOffPreference) {
            settings.setStopOnScreenOff(selected);
        } else if (preference == colorFixPreference) {
            settings.setColorFix(selected);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == outputDirPreference) {
            openOutputDirChooser();
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_OUTPUT_DIR) {
            if (resultCode == Activity.RESULT_OK) {
                settings.setOutputDir(new File(data.getData().getPath()));
                outputDirPreference.setSummary(settings.getOutputDir().getAbsolutePath());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        settings.getAudioDriver().removeInstallListener(this);
    }

    @Override
    public void onInstall(InstallationStatus status) {
        updateValues();
    }
}
