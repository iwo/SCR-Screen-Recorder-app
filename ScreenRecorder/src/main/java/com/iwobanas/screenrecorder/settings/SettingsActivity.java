package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableRow;
import android.widget.TextView;

import com.iwobanas.screenrecorder.DialogActivity;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.RecorderService;

import java.text.DecimalFormat;

public class SettingsActivity extends Activity {
    public static final String TAG = "scr_SettingsActivity";

    private TextView audioText;

    private TextView resolutionText;

    private TextView frameRateText;

    private TextView transformationText;

    private TextView videoBitrateText;

    private TextView samplingRateText;

    private CheckBox colorFixCheckBox;

    private CheckBox hideIconCheckBox;

    private CheckBox showTouchesCheckBox;

    private Button resetButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        //buyDialogOpen = false;

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
        resetButton = (Button) findViewById(R.id.settings_reset_button);

        TableRow audioRow = (TableRow) findViewById(R.id.settings_audio_row);
        audioRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AudioDialogFragment().show(getFragmentManager(), "audio");
            }
        });

        TableRow resolutionRow = (TableRow) findViewById(R.id.settings_resolution_row);
        resolutionRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ResolutionDialogFragment().show(getFragmentManager(), "resolution");
            }
        });

        TableRow frameRateRow = (TableRow) findViewById(R.id.settings_frame_rate_row);
        frameRateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FrameRateDialogFragment().show(getFragmentManager(), "frameRate");
            }
        });

        TableRow videoBitrateRow = (TableRow) findViewById(R.id.settings_video_bitrate_row);
        videoBitrateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new VideoBitrateDialogFragment().show(getFragmentManager(), "video_bitrate");
            }
        });

        TableRow samplingRateRow = (TableRow) findViewById(R.id.settings_sampling_rate_row);
        samplingRateRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SamplingRateDialogFragment().show(getFragmentManager(), "sampling_rate");
            }
        });

        TableRow transformationRow = (TableRow) findViewById(R.id.settings_transformation_row);
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
                    SettingsActivity activity = SettingsActivity.this;
                    Intent intent = new Intent(activity, DialogActivity.class);
                    intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.hide_icon_message));
                    intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.hide_icon_title));
                    intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.free_timeout_buy));
                    intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.free_timeout_no_thanks));
                    intent.putExtra(DialogActivity.RESTART_EXTRA, true);
                    intent.putExtra(DialogActivity.RESTART_EXTRA_EXTRA, RecorderService.HIDE_ICON_DIALOG_CLOSED_EXTRA);
                    startActivity(intent);
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

        refreshValues();

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Settings.getInstance().restoreDefault();
                refreshValues();
            }
        });

    }

    private void refreshValues() {
        Settings settings = Settings.getInstance();
        if (audioText != null) {
            String audioSource = settings.getAudioSource() == AudioSource.MIC ?
                    getString(R.string.settings_audio_mic) : getString(R.string.settings_audio_mute);
            audioText.setText(audioSource);
        }

        if (resolutionText != null) {
            Resolution resolution = settings.getResolution();
            if (resolution == null)
                resolution = settings.getDefaultResolution();

            resolutionText.setText(String.format(getString(R.string.settings_resolution_short),
                    resolution.getWidth(), resolution.getHeight()));
        }

        if (frameRateText != null) {
            int frameRate = settings.getFrameRate();
            if (frameRate == -1) {
                frameRateText.setText(R.string.settings_frame_rate_max_short);
            } else {
                DecimalFormat format = new DecimalFormat(getString(R.string.settings_frame_rate_up_to_short));
                frameRateText.setText(format.format(frameRate));
            }
        }

        if (transformationText != null) {
            String transformation = null;
            switch (settings.getTransformation()) {
                case CPU:
                    transformation = getString(R.string.settings_transformation_cpu);
                    break;
                case GPU:
                    transformation = getString(R.string.settings_transformation_gpu);
                    break;
                case OES:
                    transformation = getString(R.string.settings_transformation_oes);
                    break;
            }
            transformationText.setText(transformation);
        }

        if (videoBitrateText != null) {
            videoBitrateText.setText(settings.getVideoBitrate().getLabel());
        }

        if (samplingRateText != null) {
            samplingRateText.setText(settings.getSamplingRate().getLabel());
        }

        if (colorFixCheckBox != null) {
            colorFixCheckBox.setChecked(settings.getColorFix());
        }

        if (hideIconCheckBox != null) {
            hideIconCheckBox.setChecked(settings.getHideIcon());
        }

        if (showTouchesCheckBox != null) {
            showTouchesCheckBox.setChecked(settings.getShowTouches());
        }

    }

    public void settingsChanged() {
        refreshValues();
    }

}


