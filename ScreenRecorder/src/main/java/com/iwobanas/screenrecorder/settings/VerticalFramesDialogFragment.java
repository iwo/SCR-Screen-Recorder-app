package com.iwobanas.screenrecorder.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.iwobanas.screenrecorder.R;

public class VerticalFramesDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.settings_vertical_frames_warning);
        builder.setPositiveButton(R.string.settings_vertical_frames_ok, null);

        builder.setNegativeButton(R.string.settings_vertical_frames_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.getInstance().setVerticalFrames(false);
                ((SettingsActivity) getActivity()).settingsChanged();
            }
        });
        return builder.create();
    }
}
