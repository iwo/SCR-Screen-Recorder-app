package com.iwobanas.screenrecorder.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import com.iwobanas.screenrecorder.R;

public class SamplingRateDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.settings_sampling_rate);
        final SamplingRate[] samplingRates = SamplingRate.values();
        final String[] items = new String[samplingRates.length];

        for (int i = 0; i < samplingRates.length; i++) {
            items[i] = samplingRates[i].getLabel();
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.getInstance().setSamplingRate(samplingRates[i]);
                ((SettingsActivity) getActivity()).settingsChanged();
            }
        });
        return builder.create();
    }
}
