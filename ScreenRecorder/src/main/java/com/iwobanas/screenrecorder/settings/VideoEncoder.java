package com.iwobanas.screenrecorder.settings;

import android.os.Build;

import com.iwobanas.screenrecorder.Utils;

@SuppressWarnings("UnusedDeclaration")
public class VideoEncoder {
    public static final int FFMPEG_MPEG_4 = -2;
    public static final int DEFAULT = 0;
    public static final int H263 = 1;
    public static final int H264 = 2;
    public static final int MPEG_4_SP = 3;
    public static final int VP8 = 4;

    public static final int NO_ROOT_H264 = 102;
    public static final int NO_ROOT_MPEG_4 = 103;

    public static Integer[] getAllSupportedEncoders(boolean noRootOnly) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (Utils.isX86()) {
                return new Integer[]{H264, MPEG_4_SP};
            }
            return new Integer[]{H264, FFMPEG_MPEG_4, MPEG_4_SP};
        } else {
            if (noRootOnly) {
                return new Integer[]{NO_ROOT_H264};
            }
            if (Utils.isX86()) {
                return new Integer[]{NO_ROOT_H264, H264, MPEG_4_SP};
            }
            return new Integer[]{NO_ROOT_H264, H264, FFMPEG_MPEG_4, MPEG_4_SP};
        }
    }

    public static boolean isSoftware(int encoder) {
        return encoder < 0;
    }

    public static boolean isNoRoot(int encoder) {
        return encoder >= 100;
    }

    public static int getNoRootVariant(int videoEncoder) {
        // don't return NO_ROOT_MPEG_4 for now as it doesn't work on current 5.0 releases

        return NO_ROOT_H264;
    }

    public static int getRootVariant(int videoEncoder) {
        if (!isNoRoot(videoEncoder))
            return videoEncoder;

        if (videoEncoder == NO_ROOT_MPEG_4)
            return MPEG_4_SP;

        return H264;
    }
}
