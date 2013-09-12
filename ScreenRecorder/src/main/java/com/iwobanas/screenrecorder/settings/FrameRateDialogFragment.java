package com.iwobanas.screenrecorder.settings;

import android.os.Build;

import com.iwobanas.screenrecorder.R;

import java.text.DecimalFormat;

public class FrameRateDialogFragment extends SettingsListDialogFragment<Integer> {

    @Override
    protected int getTitle() {
        return R.string.settings_frame_rate;
    }

    @Override
    protected Integer[] getItems() {
        if (Build.VERSION.SDK_INT < 18) {
            return  new Integer[]{-1, 30, 20, 15, 10, 5};
        }
        return new Integer[]{-1, 50, 40, 30, 20, 15, 10, 5};
    }

    @Override
    protected Integer getSelectedItem() {
        return Settings.getInstance().getFrameRate();
    }

    @Override
    protected void setSelected(Integer item) {
        Settings.getInstance().setFrameRate(item);
    }

    @Override
    protected String[] getLabels() {
        Integer[] frameRates = getItems();
        String[] labels = new String[frameRates.length];
        labels[0] = getString(R.string.settings_frame_rate_max);
        DecimalFormat upTo = new DecimalFormat(getString(R.string.settings_frame_rate_up_to));

        for (int i = 1; i < frameRates.length; i++) {
            labels[i] = upTo.format(frameRates[i]);
        }
        return labels;
    }
}
