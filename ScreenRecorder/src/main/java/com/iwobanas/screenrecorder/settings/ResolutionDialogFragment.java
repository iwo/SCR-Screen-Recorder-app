package com.iwobanas.screenrecorder.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import com.iwobanas.screenrecorder.R;

public class ResolutionDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.settings_resolution);
        final Resolution[] resolutions = Settings.getInstance().getResolutions();
        final String[] items = new String[resolutions.length];

        for (int i = 0; i < resolutions.length; i++) {
            items[i] = formatLabel(resolutions[i]);
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.getInstance().setResolution(resolutions[i]);
                ((SettingsActivity) getActivity()).settingsChanged();
            }
        });
        return builder.create();
    }

    private String formatLabel(Resolution r) {
        return r.getLabel() + " - " + Math.max(r.getWidth(), r.getHeight())
                + "x" + Math.min(r.getWidth(), r.getHeight());
    }
}
