package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.iwobanas.screenrecorder.R;


public class AudioDriverDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.internal_audio_dialog_message));
        builder.setTitle(getString(R.string.internal_audio_dialog_title));
        builder.setIcon(R.drawable.ic_launcher);

        builder.setPositiveButton(R.string.internal_audio_dialog_install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.getInstance().getAudioDriver().install();
            }
        });

        builder.setNegativeButton(R.string.internal_audio_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                restoreMute();
            }
        });

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        restoreMute();
    }

    private void restoreMute() {
        Settings.getInstance().setAudioSource(AudioSource.MUTE);
        //TODO: ensure that settings view is updated
    }
}
