package com.iwobanas.screenrecorder.settings;

import com.iwobanas.screenrecorder.R;

public class SamplingRateDialogFragment extends SettingsListDialogFragment<SamplingRate> {

    @Override
    protected int getTitle() {
        return R.string.settings_sampling_rate;
    }

    @Override
    protected SamplingRate[] getItems() {
        return SamplingRate.values();
    }

    @Override
    protected SamplingRate getSelectedItem() {
        return getSettings().getSamplingRate();
    }

    @Override
    protected void setSelected(SamplingRate item) {
        getSettings().setSamplingRate(item);
    }

    @Override
    protected String[] getLabels() {
        final SamplingRate[] samplingRates = getItems();
        final String[] labels = new String[samplingRates.length];

        for (int i = 0; i < samplingRates.length; i++) {
            labels[i] = samplingRates[i].getLabel();
        }
        return labels;
    }
}
