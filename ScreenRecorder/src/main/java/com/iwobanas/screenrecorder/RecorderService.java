package com.iwobanas.screenrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class RecorderService extends Service implements IRecorderService {

    private static final String TAG = "RecorderService";

    private IScreenOverlay mWatermark = new WatermarkOverlay(this);

    private IScreenOverlay mRecorderOverlay = new RecorderOverlay(this, this);

    private ScreenOffReceiver mScreenOffReceiver = new ScreenOffReceiver(this, this);

    private NativeProcessRunner mNativeProcessRunner = new NativeProcessRunner(this);

    private Handler mHandler;

    private File outputFile;

    private int suRetryCount = 0;

    @Override
    public void onCreate() {
        mHandler = new Handler();
        mRecorderOverlay.show();
        mNativeProcessRunner.initialize();
    }

    @Override
    public void startRecording() {
        mRecorderOverlay.hide();
        mWatermark.show();
        mScreenOffReceiver.register();
        outputFile = getOutputFile();
        mNativeProcessRunner.start(outputFile.getAbsolutePath());
    }

    private File getOutputFile() {
        //TODO: check external storage state
        File dir = new File("/sdcard", getString(R.string.output_dir)); // there are some issues with Environment.getExternalStorageDirectory() on Nexus 4
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, getString(R.string.file_name)); //TODO: add date or number at the end of file
    }

    @Override
    public void stopRecording() {
        mNativeProcessRunner.stop();
    }

    @Override
    public void openLastFile() {
        if (outputFile == null) {
            //TODO: Store last path, if it's not available disable play button
            Log.w(TAG, "Remove this message when fixed");
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(outputFile), "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void setReady(boolean ready) {
        if (ready) {
            // enable "Record" button
        } else {
            // disable "Record" button
        }
    }

    private void showRecorderOverlay() {
        mWatermark.hide();
        mRecorderOverlay.show();
        mScreenOffReceiver.unregister();

    }

    @Override
    public void recordingFinished() {
        final Context context = this;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.recording_saved_toast), outputFile.getName());
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                //TODO: enable play button
                showRecorderOverlay();
                mNativeProcessRunner.initialize();
            }
        });
    }

    @Override
    public void suRequired() {
        final Context context = this;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = getString(R.string.su_required_message);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                if (suRetryCount++ < 3) {
                    mNativeProcessRunner.initialize();
                } else {
                    stopSelf();
                }
                //TODO: display dialog and reinitialize or quit
                showRecorderOverlay();
            }
        });
    }

    @Override
    public void startupError() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //TODO: display dialog and quit
                showRecorderOverlay();
            }
        });

    }

    @Override
    public void recordingError() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //TODO: display dialog
                showRecorderOverlay();
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mWatermark.hide();
        mRecorderOverlay.hide();
        mScreenOffReceiver.unregister();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
