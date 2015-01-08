package com.iwobanas.screenrecorder.audio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Settings;

import static com.iwobanas.screenrecorder.Tracker.ACTION;
import static com.iwobanas.screenrecorder.Tracker.AUDIO_WARNING;

public class AudioWarningDialogFragment extends DialogFragment {

    public static final String FRAGMENT_TAG = "AudioWarningDialogFragment";

    private boolean install;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_DeviceDefault_Dialog);
        View view = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.internal_audio_warning, null);
        builder.setView(view);
        builder.setTitle(R.string.internal_audio_warning_title);
        builder.setIcon(R.drawable.ic_launcher);
        TextView messageView = (TextView) view.findViewById(R.id.message);
        messageView.setText(getString(R.string.internal_audio_warning_message, getString(R.string.app_name)).replaceAll("\\s*\\n\\s*", " ").trim());

        final Settings settings = Settings.getInstance();
        final CheckBox confirmationCheckBox = (CheckBox) view.findViewById(R.id.confirmation_checkbox);
        final CheckBox doNotShowCheckBox = (CheckBox) view.findViewById(R.id.do_not_show_checkbox);

        builder.setPositiveButton(R.string.internal_audio_warning_install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                install = true;
                if (doNotShowCheckBox.isChecked()) {
                    settings.setDisableAudioWarning(true);
                }
                settings.getAudioDriver().install();
            }
        });
        builder.setNegativeButton(R.string.settings_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetToMute();
            }
        });
        final AlertDialog dialog = builder.create();


        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(confirmationCheckBox.isChecked());
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!doNotShowCheckBox.isChecked());
            }
        });


        confirmationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked);
            }
        });

        doNotShowCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!isChecked);
            }
        });

        return dialog;
    }

    public void cancel() {
        resetToMute();
        dismiss();
    }

    private void resetToMute() {
        Settings.getInstance().setAudioSource(AudioSource.MUTE);
        Toast.makeText(getActivity(), getString(R.string.internal_audio_warning_toast, getString(R.string.settings_audio_mute)), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        resetToMute();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        EasyTracker.getTracker().sendEvent(ACTION, AUDIO_WARNING, AUDIO_WARNING, install ? 1l : 0l);

        Activity activity = getActivity();
        if (activity instanceof AudioWarningActivity) {
            activity.finish();
        }
    }
}
