package com.iwobanas.screenrecorder.settings;

import com.iwobanas.screenrecorder.R;

public class VideoBitrateDialogFragment extends SettingsListDialogFragment<VideoBitrate> {

    @Override
    protected int getTitle() {
        return R.string.settings_video_bitrate;
    }

    @Override
    protected VideoBitrate[] getItems() {
        return VideoBitrate.values();
    }

    @Override
    protected VideoBitrate getSelectedItem() {
        return getSettings().getVideoBitrate();
    }

    @Override
    protected void setSelected(VideoBitrate item) {
        getSettings().setVideoBitrate(item);
    }

    @Override
    protected String[] getLabels() {
        final VideoBitrate[] bitrates = getItems();
        final String[] labels = new String[bitrates.length];

        for (int i = 0; i < bitrates.length; i++) {
            labels[i] = bitrates[i].getLabel();
        }
        return labels;
    }
}
