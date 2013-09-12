package com.iwobanas.screenrecorder.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.iwobanas.screenrecorder.R;

public abstract class SettingsListDialogFragment<T> extends DialogFragment {

    protected abstract int getTitle();

    protected abstract T[] getItems();

    protected abstract T getSelectedItem();

    protected abstract void setSelected(T item);

    protected abstract String[] getLabels();

    protected Settings getSettings() {
        return Settings.getInstance();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getTitle());

        final T[] items = getItems();
        T selectedItem = getSelectedItem();
        int selectedIndex = -1;

        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(selectedItem)) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(getLabels(), selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setSelected(items[i]);
                ((SettingsActivity) getActivity()).settingsChanged();
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.settings_cancel), null);
        return builder.create();
    }
}
