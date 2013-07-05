package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TableRow;
import android.widget.TextView;

import com.iwobanas.screenrecorder.DialogActivity;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.RecorderService;

import java.text.DecimalFormat;

public class SettingsActivity extends Activity {
    public static final String TAG = "SettingsActivity";

    private SettingsDialogFragment dialogFragment;

    private boolean buyDialogOpen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        buyDialogOpen = false;
        dialogFragment = new SettingsDialogFragment();
        dialogFragment.show(getFragmentManager(), "settingsDialog");
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onDestroy() {
        if (!buyDialogOpen) {
            Intent intent = new Intent(this, RecorderService.class);
            intent.putExtra(RecorderService.SETTINGS_CLOSED_EXTRA, true);
            startService(intent);
        }
        super.onDestroy();
    }

    public void settingsChanged() {
        if (dialogFragment != null) {
            dialogFragment.settingsChanged();
        }
    }

    public void setBuyDialogOpen(boolean buyDialogOpen) {
        this.buyDialogOpen = buyDialogOpen;
    }

    public static class SettingsDialogFragment extends DialogFragment {

        private TextView audioText;

        private TextView resolutionText;

        private TextView frameRateText;

        private TextView transformationText;

        private CheckBox colorFixCheckBox;

        private CheckBox hideIconCheckBox;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
            AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.settings_title);

            View view = getActivity().getLayoutInflater().inflate(R.layout.settings, null);
            builder.setView(view);

            audioText = (TextView) view.findViewById(R.id.settings_audio_text);
            resolutionText = (TextView) view.findViewById(R.id.settings_resolution_text);
            frameRateText = (TextView) view.findViewById(R.id.settings_frame_rate_text);
            transformationText = (TextView) view.findViewById(R.id.settings_transformation_text);
            colorFixCheckBox = (CheckBox) view.findViewById(R.id.settings_color_fix_checkbox);
            hideIconCheckBox = (CheckBox) view.findViewById(R.id.settings_hide_icon_checkbox);

            TableRow audioRow = (TableRow) view.findViewById(R.id.settings_audio_row);
            audioRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AudioDialogFragment().show(getFragmentManager(), "audio");
                }
            });

            TableRow resolutionRow = (TableRow) view.findViewById(R.id.settings_resolution_row);
            resolutionRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ResolutionDialogFragment().show(getFragmentManager(), "resolution");
                }
            });

            TableRow frameRateRow = (TableRow) view.findViewById(R.id.settings_frame_rate_row);
            frameRateRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new FrameRateDialogFragment().show(getFragmentManager(), "frameRate");
                }
            });

            TableRow transformationRow = (TableRow) view.findViewById(R.id.settings_transformation_row);
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
                        SettingsActivity activity = (SettingsActivity) getActivity();
                        activity.setBuyDialogOpen(true);
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

            refreshValues();

            builder.setPositiveButton(R.string.settings_ok, null);

            builder.setNeutralButton(R.string.settings_reset, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Settings.getInstance().restoreDefault();
                }
            });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }

        public void settingsChanged() {
            refreshValues();
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

                resolutionText.setText(resolution.getLabel());
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
                String transformation = settings.getTransformation() == Transformation.GPU ?
                        getString(R.string.settings_transformation_gpu) : getString(R.string.settings_transformation_cpu);
                transformationText.setText(transformation);
            }

            if (colorFixCheckBox != null) {
                colorFixCheckBox.setChecked(settings.getColorFix());
            }

            if (hideIconCheckBox != null) {
                hideIconCheckBox.setChecked(settings.getHideIcon());
            }

        }

    }

}


