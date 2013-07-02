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
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.RecorderService;

import java.text.DecimalFormat;

public class SettingsActivity extends Activity {
    public static final String TAG = "SettingsActivity";

    private SettingsDialogFragment dialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
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
        Intent intent = new Intent(this, RecorderService.class);
        intent.putExtra(RecorderService.SETTINGS_CLOSED_EXTRA, true);
        startService(intent);
        super.onDestroy();
    }

    public void settingsChanged() {
        if (dialogFragment != null) {
            dialogFragment.settingsChanged();
        }
    }

    public static class SettingsDialogFragment extends DialogFragment {

        private TextView audioText;

        private TextView resolutionText;

        private TextView frameRateText;

        private TextView transformationText;

        private Switch hideIconSwitch;

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
            //hideIconSwitch = (Switch) view.findViewById(R.id.settings_hide_icon_switch);

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

            refreshValues();

            builder.setPositiveButton(R.string.settings_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

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

        }

    }

}


