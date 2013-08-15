package com.iwobanas.screenrecorder.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import com.iwobanas.screenrecorder.R;

public class VideoBitrateDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.settings_video_bitrate);
        final VideoBitrate[] bitrates = VideoBitrate.values();
        final String[] items = new String[bitrates.length];

        for (int i = 0; i < bitrates.length; i++) {
            items[i] = bitrates[i].getLabel();
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.getInstance().setVideoBitrate(bitrates[i]);
                ((SettingsActivity) getActivity()).settingsChanged();
            }
        });
        return builder.create();
    }
}
