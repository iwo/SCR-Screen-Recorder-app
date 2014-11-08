package com.iwobanas.screenrecorder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioRecord;
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

import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Orientation;
import com.iwobanas.screenrecorder.settings.Resolution;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ProjectionThread implements Runnable {

    private static final String TAG = "scr_ProjectionThread";

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private Surface surface;
    private MediaCodec audioEncoder;
    private MediaCodec videoEncoder;
    private AudioRecord audioRecord;
    private MediaMuxer muxer;
    private boolean muxerStarted;
    private boolean startTimestampInitialized;
    private long startTimestampUs;
    private IRecorderService service;
    private Handler handler;
    private Thread audioRecordThread;

    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;


    private File outputFile;
    private String videoMime;
    private int videoWidth;
    private int videoHeight;
    private int videoBitrate;
    private int frameRate;
    private int sampleRate;
    private boolean hasAudio;
    private RecordingInfo recordingInfo;

    private volatile boolean stopped;
    private volatile boolean asyncError;
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
        if (s.getOrientation() == Orientation.LANDSCAPE) {
            videoWidth = resolution.getVideoWidth();
            videoHeight = resolution.getVideoHeight();
            recordingInfo.verticalInput = 1;
            recordingInfo.rotateView = 1;
        } else {
            videoWidth = resolution.getVideoHeight();
            videoHeight = resolution.getVideoWidth();
            recordingInfo.verticalInput = 1;
            recordingInfo.rotateView = 0;
        }

        videoBitrate = s.getVideoBitrate().getBitrate();
        frameRate = s.getFrameRate();

        hasAudio = s.getAudioSource() != AudioSource.MUTE;
        sampleRate = s.getSamplingRate().getSamplingRate();

        new Thread(this).start();
    }

    private void setupVideoCodec() throws IOException {
        // Encoded video resolution matches virtual display.
        MediaFormat encoderFormat = MediaFormat.createVideoFormat(videoMime, videoWidth, videoHeight);
        encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        encoderFormat.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000);
        encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        videoEncoder = MediaCodec.createEncoderByType(videoMime);
        videoEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = videoEncoder.createInputSurface();
        videoEncoder.start();


        virtualDisplay = mediaProjection.createVirtualDisplay("SCR Screen Recorder",
                videoWidth, videoHeight, DisplayMetrics.DENSITY_HIGH,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, displayCallback, handler);
    }

    private void setupAudioCodec() throws IOException {
        // Encoded video resolution matches virtual display.
        MediaFormat encoderFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);

        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        audioEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncoder.start();

        virtualDisplay = mediaProjection.createVirtualDisplay("SCR Screen Recorder",
                videoWidth, videoHeight, DisplayMetrics.DENSITY_HIGH,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, displayCallback, handler);
    }


    private void setupAudioRecord() {
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                4 * minBufferSize);
    }

    private void startAudioRecord() {
        audioRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                try {
                    audioRecord.startRecording();
                } catch (Exception e) {
                    recordingInfo.exitValue = 506;
                    if (!destroyed)
                        service.microphoneBusyError(recordingInfo);
                    asyncError = true;
                    return;
                }
                try {
                    while (true) {
                        int index = audioEncoder.dequeueInputBuffer(10000);
                        if (index < 0) {
                            continue;
                        }
                        ByteBuffer inputBuffer = audioEncoder.getInputBuffer(index);
                        if (inputBuffer == null) {
                            if (!stopped) {
                                recordingInfo.exitValue = 512;
                                if (!destroyed)
                                    service.recordingError(recordingInfo);
                                asyncError = true;
                            }
                            return;
                        }
                        inputBuffer.clear();
                        int read = audioRecord.read(inputBuffer, inputBuffer.capacity());
                        if (read == AudioRecord.ERROR_INVALID_OPERATION) {
                            break;
                        }
                        audioEncoder.queueInputBuffer(index, 0, read, getPresentationTimeUs(), 0);
                    }
                } catch (Exception e) {
                    if (!stopped) {
                        Log.e(TAG, "Audio error", e);
                        recordingInfo.exitValue = 511;
                        if (!destroyed)
                            service.recordingError(recordingInfo);
                        asyncError = true;
                    }
                }
            }
        });
        audioRecordThread.start();
    }

    private long getPresentationTimeUs() {
        if (!startTimestampInitialized) {
            startTimestampUs = System.nanoTime() / 1000;
            startTimestampInitialized = true;
        }
        return System.nanoTime() / 1000 - startTimestampUs;
    }

    private void startMuxerIfSetUp() {
        if ((!hasAudio || audioTrackIndex >= 0) && videoTrackIndex >= 0) {
            muxer.start();
            muxerStarted = true;
            if (!destroyed)
                service.recordingStarted();
        }
    }

    @Override
    public void run() {

        try {

            try {
                setupVideoCodec();
            } catch (Exception e) {
                Log.e(TAG, "video error", e);
                recordingInfo.exitValue = 501;
                if (!destroyed)
                    service.startupError(recordingInfo);
                return;
            }

            if (hasAudio) {
                try {
                    setupAudioCodec();
                } catch (Exception e) {
                    Log.e(TAG, "audio error", e);
                    recordingInfo.exitValue = 505;
                    if (!destroyed)
                        service.startupError(recordingInfo);
                    return;
                }

                try {
                    setupAudioRecord();
                } catch (Exception e) {
                    Log.e(TAG, "AudioRecord error", e);
                    recordingInfo.exitValue = 507;
                    if (!destroyed)
                        service.audioConfigError(recordingInfo);
                    return;
                }
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

            if (hasAudio) {
                startAudioRecord();
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (!stopped && !asyncError) {

                int encoderStatus;

                if (hasAudio) {
                    encoderStatus = audioEncoder.dequeueOutputBuffer(bufferInfo, 0);
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no input frames... continue to video
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            recordingInfo.exitValue = 508;
                            break;
                        }
                        audioTrackIndex = muxer.addTrack(audioEncoder.getOutputFormat());
                        startMuxerIfSetUp();
                    } else if (encoderStatus < 0) {
                        Log.w(TAG, "unexpected result from audio encoder.dequeueOutputBuffer: " + encoderStatus);
                    } else {
                        // Normal flow: get output encoded buffer, send to muxer.
                        ByteBuffer encodedData = audioEncoder.getOutputBuffer(encoderStatus);
                        if (encodedData == null) {
                            recordingInfo.exitValue = 509;
                            break;
                        }

                        if (bufferInfo.size != 0 && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            if (!muxerStarted) {
                                throw new RuntimeException("muxer hasn't started");
                            }

                            muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo);
                        }

                        audioEncoder.releaseOutputBuffer(encoderStatus, false);

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            recordingInfo.exitValue = 510;
                            break;
                        }
                    }
                }

                encoderStatus = videoEncoder.dequeueOutputBuffer(bufferInfo, 20000);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no input frames... wait
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        recordingInfo.exitValue = 502;
                        break;
                    }
                    videoTrackIndex = muxer.addTrack(videoEncoder.getOutputFormat());
                    startMuxerIfSetUp();
                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else {
                    // Normal flow: get output encoded buffer, send to muxer.
                    ByteBuffer encodedData = videoEncoder.getOutputBuffer(encoderStatus);
                    if (encodedData == null) {
                        recordingInfo.exitValue = 503;
                        break;
                    }

                    if (bufferInfo.size != 0 && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (!muxerStarted) {
                            throw new RuntimeException("muxer hasn't started");
                        }

                        bufferInfo.presentationTimeUs = getPresentationTimeUs();
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                    }

                    videoEncoder.releaseOutputBuffer(encoderStatus, false);

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
                try {
                    muxer.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping muxer", e);
                }
                muxer = null;
            }

            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            if (virtualDisplay != null) {
                try {
                    virtualDisplay.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping display", e);
                }
                virtualDisplay = null;
            }
            if (surface != null) {
                try {
                    surface.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping surface", e);
                }
                surface = null;
            }
            if (videoEncoder != null) {
                try {
                    videoEncoder.stop();
                    videoEncoder.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping video encoder", e);
                }
                videoEncoder = null;
            }

            if (audioRecord != null) {
                try {
                    audioRecord.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping AudioRecord", e);
                }
                audioRecord = null;
            }

            if (audioEncoder != null) {
                try {
                    audioEncoder.stop();
                    audioEncoder.release();
                } catch (Exception e) {
                    Log.w(TAG, "Error stopping audio encoder", e);
                }
                audioEncoder = null;
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
