package com.iwobanas.screenrecorder.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

import com.iwobanas.screenrecorder.R;

public class TransformationDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.settings_transformation);
        String[] items = new String[]{getString(R.string.settings_transformation_gpu), getString(R.string.settings_transformation_cpu)};
        final Transformation[] options = new Transformation[]{Transformation.GPU, Transformation.CPU};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Settings.getInstance().setTransformation(options[i]);
                ((SettingsActivity) getActivity()).settingsChanged();
            }
        });
        return builder.create();
    }
}
