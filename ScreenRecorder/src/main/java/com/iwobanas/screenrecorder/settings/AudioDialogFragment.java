package com.iwobanas.screenrecorder.settings;

import com.iwobanas.screenrecorder.R;

public class AudioDialogFragment extends SettingsListDialogFragment<AudioSource> {

    @Override
    protected int getTitle() {
        return R.string.settings_audio;
    }

    @Override
    protected AudioSource[] getItems() {
        return AudioSource.values();
    }

    @Override
    protected AudioSource getSelectedItem() {
        return getSettings().getAudioSource();
    }

    @Override
    protected void setSelected(AudioSource item) {
        getSettings().setAudioSource(item);
    }

    @Override
    protected String[] getLabels() {
        return new String[] {getString(R.string.settings_audio_mic), getString(R.string.settings_audio_mute)};
    }
}
