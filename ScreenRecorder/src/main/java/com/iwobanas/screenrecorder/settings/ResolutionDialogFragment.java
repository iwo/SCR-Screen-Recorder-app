package com.iwobanas.screenrecorder.settings;

import com.iwobanas.screenrecorder.R;

public class ResolutionDialogFragment extends SettingsListDialogFragment<Resolution> {

    @Override
    protected int getTitle() {
        return R.string.settings_resolution;
    }

    @Override
    protected Resolution[] getItems() {
        return getSettings().getResolutions();
    }

    @Override
    protected Resolution getSelectedItem() {
        return getSettings().getResolution();
    }

    @Override
    protected void setSelected(Resolution item) {
        getSettings().setResolution(item);
    }

    @Override
    protected String[] getLabels() {
        Resolution[] resolutions = getItems();
        String[] labels = new String[resolutions.length];
        for (int i = 0; i < resolutions.length; i++) {
            Resolution resolution = resolutions[i];
            labels[i] = formatLabel(resolution);
        }
        return labels;
    }

    private String formatLabel(Resolution r) {
        String label = String.format(getString(r.getLabelId(), r.getWidth(), r.getHeight()));
        if (r.getHeight() > 720) {
            label += " " + getString(R.string.settings_resolution_unstable);
        }
        return label;
    }
}
