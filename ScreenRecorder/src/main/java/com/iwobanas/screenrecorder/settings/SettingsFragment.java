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
import android.preference.PreferenceFragment;
import android.util.Log;

import com.iwobanas.screenrecorder.DirectoryChooserActivity;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.audio.AudioDriver;
import com.iwobanas.screenrecorder.audio.InstallationStatus;

import java.io.File;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, AudioDriver.OnInstallListener {
    public static final String KEY_COPYRIGHTS_STATEMENT = "copyrights_statement";
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
    public static final String KEY_SHOW_CAMERA = "show_camera";
    public static final String KEY_CAMERA_ALPHA = "camera_alpha";
    public static final String KEY_OUTPUT_DIR = "output_dir";
    public static final String KEY_STOP_ON_SCREEN_OFF = "stop_on_screen_off";
    public static final String KEY_COLOR_FIX = "color_fix";
    private static final int SELECT_OUTPUT_DIR = 1;
    private static final String TAG = "scr_SettingsFragment";
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
    private CheckBoxPreference showCameraPreference;
    private SliderPreference cameraAlphaPreference;
    private Preference outputDirPreference;
    private CheckBoxPreference stopOnScreenOffPreference;
    private CheckBoxPreference colorFixPreference;
    private Settings settings;
    private boolean outputDirChooserOpen = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        String copyrightsStatement = getString(R.string.copyrights_statement, getString(R.string.app_name));
        findPreference(KEY_COPYRIGHTS_STATEMENT).setSummary(copyrightsStatement);

        settings = Settings.getInstance();

        videoEncoderPreference = (ListPreference) findPreference(KEY_VIDEO_ENCODER);
        videoEncoderPreference.setOnPreferenceChangeListener(this);
        if (Utils.isX86()) {
            videoEncoderPreference.setEntries(R.array.video_encoder_entries_no_sw);
            videoEncoderPreference.setEntryValues(R.array.video_encoder_values_no_sw);
        }

        resolutionPreference = (ListPreference) findPreference(KEY_RESOLUTION);
        resolutionPreference.setOnPreferenceChangeListener(this);
        resolutionPreference.setEntries(getResolutionEntries());
        resolutionPreference.setEntryValues(getResolutionEntryValues());

        transformationPreference = (ListPreference) findPreference(KEY_TRANSFORMATION);
        if (Build.VERSION.SDK_INT < 18) {
            transformationPreference.setEntries(R.array.transformation_entries_no_oes);
            transformationPreference.setEntryValues(R.array.transformation_values_no_oes);
        }
        transformationPreference.setOnPreferenceChangeListener(this);

        videoBitratePreference = (ListPreference) findPreference(KEY_VIDEO_BITRATE);
        videoBitratePreference.setOnPreferenceChangeListener(this);

        frameRatePreference = (ListPreference) findPreference(KEY_FRAME_RATE);
        if (Build.VERSION.SDK_INT < 18) {
            frameRatePreference.setEntryValues(R.array.frame_rate_values_no_oes);
        }
        frameRatePreference.setEntries(getFrameRateEntries(frameRatePreference.getEntryValues()));
        frameRatePreference.setOnPreferenceChangeListener(this);

        verticalFramesPreference = (CheckBoxPreference) findPreference(KEY_VERTICAL_FRAMES);
        verticalFramesPreference.setOnPreferenceChangeListener(this);

        audioSourcePreference = (ListPreference) findPreference(KEY_AUDIO_SOURCE);
        audioSourcePreference.setOnPreferenceChangeListener(this);
        if (Build.VERSION.SDK_INT > 15 && Build.VERSION.SDK_INT != 17) {
            audioSourcePreference.setEntries(R.array.audio_source_entries_internal);
            audioSourcePreference.setEntryValues(R.array.audio_source_values_internal);
        }
        samplingRatePreference = (ListPreference) findPreference(KEY_SAMPLING_RATE);
        samplingRatePreference.setOnPreferenceChangeListener(this);

        hideIconPreference = (CheckBoxPreference) findPreference(KEY_HIDE_ICON);
        hideIconPreference.setOnPreferenceChangeListener(this);

        showTouchesPreference = (CheckBoxPreference) findPreference(KEY_SHOW_TOUCHES);
        showTouchesPreference.setOnPreferenceChangeListener(this);

        showCameraPreference = (CheckBoxPreference) findPreference(KEY_SHOW_CAMERA);
        showCameraPreference.setOnPreferenceChangeListener(this);

        cameraAlphaPreference = (SliderPreference) findPreference(KEY_CAMERA_ALPHA);
        cameraAlphaPreference.setOnPreferenceChangeListener(this);

        outputDirPreference = findPreference(KEY_OUTPUT_DIR);
        outputDirPreference.setOnPreferenceClickListener(this);

        stopOnScreenOffPreference = (CheckBoxPreference) findPreference(KEY_STOP_ON_SCREEN_OFF);
        stopOnScreenOffPreference.setOnPreferenceChangeListener(this);

        colorFixPreference = (CheckBoxPreference) findPreference(KEY_COLOR_FIX);
        colorFixPreference.setOnPreferenceChangeListener(this);

        settings.getAudioDriver().addInstallListener(this);
        updateValues();
    }

    protected void updateValues() {
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
        InstallationStatus installationStatus = settings.getAudioDriver().getInstallationStatus();
        samplingRatePreference.setValue(settings.getSamplingRate().name());
        samplingRatePreference.setSummary(formatSamplingRateSummary());
        samplingRatePreference.setEnabled(settings.getAudioSource().equals(AudioSource.MIC));

        hideIconPreference.setChecked(settings.getHideIcon());
        showTouchesPreference.setChecked(settings.getShowTouches());
        showCameraPreference.setChecked(settings.getShowCamera());
        cameraAlphaPreference.setValue((int) (settings.getCameraAlpha() * 100));
        cameraAlphaPreference.setSummary(formatCameraAlphaSummary());
        outputDirPreference.setSummary(settings.getOutputDir().getAbsolutePath());
        stopOnScreenOffPreference.setChecked(settings.getStopOnScreenOff());
        colorFixPreference.setChecked(settings.getColorFix());
    }

    private String formatCameraAlphaSummary() {
        if (getActivity() == null) return "";
        if (settings.getCameraAlpha() == 1.0f) {
            return getString(R.string.settings_camera_alpha_summary_100);
        }
        int percentage = (int) (settings.getCameraAlpha() * 100);
        return getString(R.string.settings_camera_alpha_summary, percentage);
    }

    private String formatVideoEncoderSummary(int videoEncoder) {
        switch (videoEncoder) {
            case Settings.FFMPEG_MPEG_4_ENCODER:
                return getString(R.string.settings_video_encoder_ffmpeg_summary);
            case MediaRecorder.VideoEncoder.H264:
                return String.format(getString(
                        R.string.settings_video_encoder_built_in_summary),
                        getString(R.string.settings_video_encoder_h264));
            case MediaRecorder.VideoEncoder.MPEG_4_SP:
                return String.format(getString(
                        R.string.settings_video_encoder_built_in_summary),
                        getString(R.string.settings_video_encoder_mpeg_4_sp));
        }
        return "";
    }

    private CharSequence[] getResolutionEntries() {
        Resolution[] resolutions = settings.getResolutions();
        String[] entries = new String[resolutions.length];
        for (int i = 0; i < resolutions.length; i++) {
            Resolution resolution = resolutions[i];
            entries[i] = formatResolutionEntry(resolution);
        }
        return entries;
    }

    private String formatResolutionEntry(Resolution r) {
        String entry = String.format(getString(r.getLabelId(), r.getWidth(), r.getHeight()));
        if (r.getHeight() > 720) {
            entry += " " + getString(R.string.settings_resolution_unstable);
        }
        return entry;
    }

    private CharSequence[] getResolutionEntryValues() {
        Resolution[] resolutions = settings.getResolutions();
        String[] values = new String[resolutions.length];
        for (int i = 0; i < resolutions.length; i++) {
            Resolution resolution = resolutions[i];
            values[i] = formatResolutionEntryValue(resolution);
        }
        return values;
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
        outputDirChooserOpen = true;
        startActivityForResult(intent, SELECT_OUTPUT_DIR);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String valueString = null;
        Boolean selected = null;
        if (preference instanceof ListPreference) {
            valueString = (String) newValue;
        }
        if (preference instanceof CheckBoxPreference) {
            selected = (Boolean) newValue;
        }

        if (preference == videoEncoderPreference) {
            int videoEncoder = Integer.parseInt(valueString);
            settings.setVideoEncoder(videoEncoder);
            updateValues();

        } else if (preference == resolutionPreference) {
            Resolution resolution = findResolution(valueString);
            settings.setResolution(resolution);
            preference.setSummary(formatResolutionEntry(resolution));

        } else if (preference == transformationPreference) {
            Transformation transformation = Transformation.valueOf(valueString);
            settings.setTransformation(transformation);
            preference.setSummary(formatTransformationSummary(transformation));

        } else if (preference == videoBitratePreference) {
            VideoBitrate bitrate = VideoBitrate.valueOf(valueString);
            settings.setVideoBitrate(bitrate);
            preference.setSummary(formatVideoBitrateSummary(bitrate));

        } else if (preference == frameRatePreference) {
            int frameRate = Integer.parseInt(valueString);
            settings.setFrameRate(frameRate);
            preference.setSummary(formatFrameRateSummary(frameRate));

        } else if (preference == verticalFramesPreference) {
            settings.setVerticalFrames(selected);
        } else if (preference == audioSourcePreference) {
            AudioSource source = AudioSource.valueOf(valueString);
            settings.setAudioSource(source);
            updateValues();
        } else if (preference == samplingRatePreference) {
            SamplingRate rate = SamplingRate.valueOf(valueString);
            settings.setSamplingRate(rate);
            preference.setSummary(rate.getLabel());
        } else if (preference == hideIconPreference) {
            if (getResources().getBoolean(R.bool.taniosc)) {
                new HideIconDialogFragment().show(getFragmentManager(), "hideWatermark");
                return false;
            } else {
                settings.setHideIcon(selected);
            }
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
            outputDirChooserOpen = false;
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
