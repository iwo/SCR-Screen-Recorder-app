package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableRow;
import android.widget.TextView;

import com.iwobanas.screenrecorder.DirectoryChooserActivity;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.ReportBugTask;

import java.io.File;
import java.text.DecimalFormat;

public class SettingsActivity extends Activity {
    public static final String TAG = "scr_SettingsActivity";
    private static final int SELECT_OUTPUT_DIR = 1;
    private TextView audioText;
    private TextView resolutionText;
    private TextView frameRateText;
    private TextView transformationText;
    private TextView videoBitrateText;
    private TextView samplingRateText;
    private CheckBox colorFixCheckBox;
    private CheckBox hideIconCheckBox;
    private CheckBox showTouchesCheckBox;
    private CheckBox stopOnScreenOffCheckBox;
    private TextView outputDirText;
    private TextView videoEncoderText;
    private CheckBox verticalFramesCheckBox;
    private TableRow audioRow;
    private TableRow resolutionRow;
    private TableRow frameRateRow;
    private TableRow videoBitrateRow;
    private TableRow samplingRateRow;
    private TableRow transformationRow;
    private TableRow outputDirRow;
    private TableRow videoEncoderRow;
    private TableRow verticalFramesRow;
    private boolean viewsInitialized = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        audioText = (TextView) findViewById(R.id.settings_audio_text);
        resolutionText = (TextView) findViewById(R.id.settings_resolution_text);
        frameRateText = (TextView) findViewById(R.id.settings_frame_rate_text);
        transformationText = (TextView) findViewById(R.id.settings_transformation_text);
        videoBitrateText = (TextView) findViewById(R.id.settings_video_bitrate_text);
        samplingRateText = (TextView) findViewById(R.id.settings_sampling_rate_text);
        colorFixCheckBox = (CheckBox) findViewById(R.id.settings_color_fix_checkbox);
        hideIconCheckBox = (CheckBox) findViewById(R.id.settings_hide_icon_checkbox);
        showTouchesCheckBox = (CheckBox) findViewById(R.id.settings_show_touches_checkbox);
        stopOnScreenOffCheckBox = (CheckBox) findViewById(R.id.settings_stop_on_screen_off_checkbox);
        outputDirText = (TextView) findViewById(R.id.settings_output_dir_text);
        videoEncoderText = (TextView) findViewById(R.id.settings_video_encoder_text);
        verticalFramesRow = (TableRow) findViewById(R.id.settings_vertical_frames_row);

        audioRow = (TableRow) findViewById(R.id.settings_audio_row);
        resolutionRow = (TableRow) findViewById(R.id.settings_resolution_row);
        frameRateRow = (TableRow) findViewById(R.id.settings_frame_rate_row);
        videoBitrateRow = (TableRow) findViewById(R.id.settings_video_bitrate_row);
        samplingRateRow = (TableRow) findViewById(R.id.settings_sampling_rate_row);
        transformationRow = (TableRow) findViewById(R.id.settings_transformation_row);
        outputDirRow = (TableRow) findViewById(R.id.settings_output_dir_row);
        videoEncoderRow = (TableRow) findViewById(R.id.settings_video_encoder_row);
        verticalFramesCheckBox = (CheckBox) findViewById(R.id.settings_vertical_frames_checkbox);

        viewsInitialized = true;

        audioRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AudioDialogFragment().show(getFragmentManager(), "audio");
            }
        });
        resolutionRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ResolutionDialogFragment().show(getFragmentManager(), "resolution");
            }
        });
        frameRateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FrameRateDialogFragment().show(getFragmentManager(), "frameRate");
            }
        });
        videoBitrateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new VideoBitrateDialogFragment().show(getFragmentManager(), "video_bitrate");
            }
        });
        samplingRateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SamplingRateDialogFragment().show(getFragmentManager(), "sampling_rate");
            }
        });
        transformationRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new TransformationDialogFragment().show(getFragmentManager(), "transformation");
            }
        });
        colorFixCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                Settings.getInstance().setColorFix(checked);
                refreshValues();
            }
        });
        hideIconCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (getResources().getBoolean(R.bool.taniosc)) {
                    new HideIconDialogFragment().show(getFragmentManager(), "hideWatermark");
                    compoundButton.setChecked(false);
                } else {
                    Settings.getInstance().setHideIcon(checked);
                }
                refreshValues();
            }
        });
        showTouchesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                Settings.getInstance().setShowTouches(checked);
            }
        });
        stopOnScreenOffCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                Settings.getInstance().setStopOnScreenOff(checked);
            }
        });
        outputDirRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this, DirectoryChooserActivity.class);
                intent.setData(Uri.fromFile(Settings.getInstance().getOutputDir()));
                intent.putExtra(DirectoryChooserActivity.DEFAULT_DIR_EXTRA, Settings.getInstance().getDefaultOutputDir().getAbsolutePath());
                startActivityForResult(intent, SELECT_OUTPUT_DIR);
            }
        });
        videoEncoderRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new VideoEncoderDialogFragment().show(getFragmentManager(), "videoEncoder");
            }
        });
        verticalFramesCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked == Settings.getInstance().getVerticalFrames()) {
                    return;
                }
                Settings.getInstance().setVerticalFrames(checked);
                if (checked) {
                    new VerticalFramesDialogFragment().show(getFragmentManager(), "verticalFrames");
                }
            }
        });

        refreshValues();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_restore_defaults:
                Settings.getInstance().restoreDefault();
                refreshValues();
                return true;
            case R.id.settings_send_bug_report:
                sendBugReport();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendBugReport() {
        new ReportBugTask(getApplicationContext(), 1000).execute();
    }

    private void refreshValues() {
        if (!viewsInitialized) return;

        Settings settings = Settings.getInstance();

        audioText.setText(getAudioSourceLabel(settings.getAudioSource()));
        resolutionText.setText(getResolutionLabel(settings.getResolution(), settings.getDefaultResolution()));
        frameRateText.setText(getFrameRateLabel(settings.getFrameRate()));
        transformationText.setText(getTransformationLabel(settings.getTransformation()));
        videoBitrateText.setText(settings.getVideoBitrate().getLabel());
        samplingRateText.setText(settings.getSamplingRate().getLabel());
        colorFixCheckBox.setChecked(settings.getColorFix());
        hideIconCheckBox.setChecked(settings.getHideIcon());
        showTouchesCheckBox.setChecked(settings.getShowTouches());
        stopOnScreenOffCheckBox.setChecked(settings.getStopOnScreenOff());
        outputDirText.setText(settings.getOutputDir().getAbsolutePath());
        videoEncoderText.setText(getVideoEncoderLabel(settings.getVideoEncoder()));
        verticalFramesCheckBox.setChecked(settings.getVerticalFrames());

        if (settings.getVideoEncoder() == Settings.FFMPEG_MPEG_4_ENCODER) {
            transformationRow.setVisibility(View.GONE);
        } else {
            transformationRow.setVisibility(View.VISIBLE);
        }

        if (AudioSource.MUTE.equals(settings.getAudioSource())) {
            samplingRateRow.setVisibility(View.GONE);
        } else {
            samplingRateRow.setVisibility(View.VISIBLE);
        }
    }

    private String getAudioSourceLabel(AudioSource audioSource) {
        return audioSource == AudioSource.MIC ?
                getString(R.string.settings_audio_mic)
                : getString(R.string.settings_audio_mute);
    }

    private String getResolutionLabel(Resolution resolution, Resolution defaultResolution) {
        if (resolution == null) {
            resolution = defaultResolution;
        }
        return String.format(getString(R.string.settings_resolution_short), resolution.getWidth(), resolution.getHeight());
    }

    private String getFrameRateLabel(int frameRate) {
        if (frameRate == -1) {
            return getString(R.string.settings_frame_rate_max_short);
        }
        DecimalFormat format = new DecimalFormat(getString(R.string.settings_frame_rate_up_to_short));
        return format.format(frameRate);
    }

    private String getTransformationLabel(Transformation transformation) {
        switch (transformation) {
            case CPU:
                return getString(R.string.settings_transformation_cpu);
            case GPU:
                return getString(R.string.settings_transformation_gpu);
            case OES:
                return getString(R.string.settings_transformation_oes);
        }
        return "";
    }

    private String getVideoEncoderLabel(int videoEncoder) {
        switch (videoEncoder) {
            case MediaRecorder.VideoEncoder.DEFAULT:
                return getString(R.string.settings_video_encoder_default);
            case MediaRecorder.VideoEncoder.H264:
                return getString(R.string.settings_video_encoder_h264);
            case MediaRecorder.VideoEncoder.H263:
                return getString(R.string.settings_video_encoder_h263);
            case MediaRecorder.VideoEncoder.MPEG_4_SP:
                return getString(R.string.settings_video_encoder_mpeg_4_sp);
            case Settings.FFMPEG_MPEG_4_ENCODER:
                return getString(R.string.settings_video_encoder_ffmpeg_mpeg_4_short);
        }
        return "";
    }

    public void settingsChanged() {
        refreshValues();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_OUTPUT_DIR) {

            if (resultCode == RESULT_OK) {
                Settings.getInstance().setOutputDir(new File(data.getData().getPath()));
                refreshValues();
            }
        }
    }
}


