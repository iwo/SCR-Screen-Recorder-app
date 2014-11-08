package com.iwobanas.screenrecorder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.iwobanas.screenrecorder.settings.Resolution;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.File;
import java.nio.ByteBuffer;

public class ProjectionThread implements Runnable {

    private static final String TAG = "scr_ProjectionThread";

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private Surface surface;
    private MediaCodec encoder;
    private MediaMuxer muxer;
    private boolean muxerStarted;
    private boolean startTimestampInitialized;
    private long startTimestampUs;
    private IRecorderService service;
    private Handler handler;

    private File outputFile;
    private String videoMime;
    private int videoWidth;
    private int videoHeight;
    private int videoBitrate;
    private int frameRate;
    private RecordingInfo recordingInfo;

    private volatile boolean stopped;
    private volatile boolean destroyed;

    private VirtualDisplay.Callback displayCallback = new VirtualDisplay.Callback() {
        @Override
        public void onPaused() {
            super.onPaused();
            stopRecording();
        }

        @Override
        public void onStopped() {
            super.onStopped();
            stopRecording();
        }
    };

    public ProjectionThread(MediaProjection mediaProjection, IRecorderService service) {
        this.mediaProjection = mediaProjection;
        this.service = service;
        handler = new Handler();
    }

    public void startRecording(File outputFile) {
        this.outputFile = outputFile;
        recordingInfo = new RecordingInfo();
        recordingInfo.fileName = outputFile.getAbsolutePath();

        //TODO: report all caught exceptions to analytics

        Settings s = Settings.getInstance();

        switch (s.getVideoEncoder()) {
            case MediaRecorder.VideoEncoder.H264:
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC;
                break;
            case MediaRecorder.VideoEncoder.MPEG_4_SP:
                videoMime = MediaFormat.MIMETYPE_VIDEO_MPEG4;
                break;
            default:
                // use default encoder
                videoMime = MediaFormat.MIMETYPE_VIDEO_AVC;
        }
        Resolution resolution = s.getResolution();
        videoWidth = resolution.getVideoWidth();
        videoHeight = resolution.getVideoHeight();

        videoBitrate = s.getVideoBitrate().getBitrate();
        frameRate = s.getFrameRate();

        new Thread(this).start();
    }

    @Override
    public void run() {

        try {
            // Encoded video resolution matches virtual display.
            MediaFormat encoderFormat = MediaFormat.createVideoFormat(videoMime, videoWidth, videoHeight);
            encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
            encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            encoderFormat.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000);
            encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

            try {
                encoder = MediaCodec.createEncoderByType(videoMime);
                encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                surface = encoder.createInputSurface();
                encoder.start();


                virtualDisplay = mediaProjection.createVirtualDisplay("SCR Screen Recorder",
                        videoWidth, videoHeight, DisplayMetrics.DENSITY_HIGH,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        surface, displayCallback, handler);

            } catch (Exception e) {
                Log.e(TAG, "Startup error", e);
                recordingInfo.exitValue = 501;
                if (!destroyed)
                    service.startupError(recordingInfo);
                return;
            }

            try {
                muxer = new MediaMuxer(outputFile.getAbsolutePath(),
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } catch (Exception e) {
                Log.e(TAG, "Muxer error", e);
                recordingInfo.exitValue = 201;
                if (!destroyed)
                    service.outputFileError(recordingInfo);
                return;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int videoTrackIndex = -1;

            while (!stopped) {
                int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 250000);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no input frames... wait
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    /**
                     * should happen before receiving buffers, and should only
                     * happen once
                     */
                    if (muxerStarted) {
                        recordingInfo.exitValue = 502;
                        break;
                    }
                    videoTrackIndex = muxer.addTrack(encoder.getOutputFormat());
                    muxer.start();
                    muxerStarted = true;
                    if (!destroyed)
                        service.recordingStarted();
                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else {
                    // Normal flow: get output encoded buffer, send to muxer.
                    ByteBuffer encodedData = encoder.getOutputBuffer(encoderStatus);
                    if (encodedData == null) {
                        recordingInfo.exitValue = 503;
                        break;
                    }

                    if (bufferInfo.size != 0 && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (!muxerStarted) {
                            throw new RuntimeException("muxer hasn't started");
                        }

                        if (!startTimestampInitialized) {
                            startTimestampUs = System.nanoTime() / 1000;
                            startTimestampInitialized = true;
                        }
                        bufferInfo.presentationTimeUs = System.nanoTime() / 1000 - startTimestampUs;
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                    }

                    encoder.releaseOutputBuffer(encoderStatus, false);

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        recordingInfo.exitValue = 503;
                        break;
                    }
                }
            }
        } catch (Throwable throwable) {
            Log.e(TAG, "Recording error", throwable);
            recordingInfo.exitValue = 504;
        } finally {
            if (muxer != null) {
                muxer.stop();
                muxer.release();
                muxer = null;
            }

            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            if (surface != null) {
                surface.release();
                surface = null;
            }
            if (encoder != null) {
                encoder.stop();
                encoder.release();
                encoder = null;
            }
        }

        if (stopped) {
            if (!destroyed)
                service.recordingFinished(recordingInfo);
        } else {
            if (!destroyed)
                service.recordingError(recordingInfo);
        }
    }

    public void stopRecording() {
        stopped = true;
    }

    public void destroy() {
        destroyed = true;
    }

}
