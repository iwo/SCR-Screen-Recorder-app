package com.iwobanas.screenrecorder.settings;

import android.media.MediaRecorder;

import com.iwobanas.screenrecorder.R;

public class VideoEncoderDialogFragment extends SettingsListDialogFragment<Integer> {

    @Override
    protected int getTitle() {
        return R.string.settings_transformation;
    }

    @Override
    protected Integer[] getItems() {
        return new Integer[]{
                MediaRecorder.VideoEncoder.H264,
                Settings.FFMPEG_MPEG_4_ENCODER,
                MediaRecorder.VideoEncoder.MPEG_4_SP
        };
    }

    @Override
    protected Integer getSelectedItem() {
        return getSettings().getVideoEncoder();
    }

    @Override
    protected void setSelected(Integer item) {
        getSettings().setVideoEncoder(item);
    }

    @Override
    protected String[] getLabels() {
        return new String[]{
                getString(R.string.settings_video_encoder_h264),
                getString(R.string.settings_video_encoder_ffmpeg_mpeg_4),
                getString(R.string.settings_video_encoder_mpeg_4_sp)
        };
    }
}
