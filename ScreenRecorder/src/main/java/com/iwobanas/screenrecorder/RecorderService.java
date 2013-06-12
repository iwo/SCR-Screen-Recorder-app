package com.iwobanas.screenrecorder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RecorderService extends Service implements IRecorderService {

    private static final String TAG = "RecorderService";

    public static final String PREFERENCES_NAME = "ScreenRecorderPreferences";

    private static final String LAST_RECORDED_FILE_PREFERENCE = "lastRecordedFile";


    private WatermarkOverlay mWatermark = new WatermarkOverlay(this);

    private IScreenOverlay mRecorderOverlay = new RecorderOverlay(this, this);

    private ScreenOffReceiver mScreenOffReceiver = new ScreenOffReceiver(this, this);

    private NativeProcessRunner mNativeProcessRunner = new NativeProcessRunner(this);

    private Handler mHandler;

    private File outputFile;

    private int suRetryCount = 0;

    private boolean isRecording;

    // Preferences
    private String mLastRecorderFile;

    @Override
    public void onCreate() {
        mHandler = new Handler();

        readPreferences();
        installExecutable();

        mWatermark.show();
        mRecorderOverlay.show();
        mNativeProcessRunner.initialize();
    }

    private void installExecutable() {
        File executable = new File(getFilesDir(), "screenrec");
        try {
            extractResource(R.raw.screenrec, executable);
            if (!executable.setExecutable(true, false)) {
                Log.w(TAG, "Can't set executable property on " + executable.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "Can't install native executable", e);
            //TODO: indicate installation error
        }
        mNativeProcessRunner.setExecutable(executable.getAbsolutePath());
    }

    private void extractResource(int resourceId, File outputFile) throws IOException {
        InputStream inputStream = getResources().openRawResource(resourceId);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        int count;
        byte[] buffer = new byte[1024];
        while ((count = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, count);
        }
        inputStream.close();
        outputStream.close();
    }

    @Override
    public void startRecording() {
        mRecorderOverlay.hide();
        mWatermark.start();
        mScreenOffReceiver.register();
        outputFile = getOutputFile();
        isRecording = true;
        mNativeProcessRunner.start(outputFile.getAbsolutePath(), getRotation());
    }

    private File getOutputFile() {
        //TODO: check external storage state
        File dir = new File("/sdcard", getString(R.string.output_dir)); // there are some issues with Environment.getExternalStorageDirectory() on Nexus 4
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.w(TAG, "mkdirs failed " + dir.getAbsolutePath());
            }
        }
        SimpleDateFormat format = new SimpleDateFormat(getString(R.string.file_name_format));
        return new File(dir, format.format(new Date()));
    }

    private String getRotation() {
        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Configuration config = getResources().getConfiguration();
        int rotationDeg = getRotationDeg(display);

        if (getDeviceDefaultOrientation(display, config) == Configuration.ORIENTATION_PORTRAIT) {
            rotationDeg = ((360 - rotationDeg) + 90) % 360;
        } else {
            rotationDeg = (360 - rotationDeg) % 360; //TODO: test on horizontal device
        }

        return String.valueOf(rotationDeg);
    }

    private int getRotationDeg(Display display) {
        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    public int getDeviceDefaultOrientation(Display display, Configuration config) {
        int rotation = display.getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }


    @Override
    public void stopRecording() {
        isRecording = false;
        mNativeProcessRunner.stop();
    }

    @Override
    public void openLastFile() {
        if (mLastRecorderFile == null) {
            //TODO: Store last path, if it's not available disable play button
            Log.w(TAG, "Remove this message when fixed");
            return;
        }
        startActivity(getPlayVideoIntent());
        stopSelf();
    }

    private Intent getPlayVideoIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mLastRecorderFile)), "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public void close() {
        stopSelf();
    }

    @Override
    public void setReady(boolean ready) {
    }

    private void showRecorderOverlay() {
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
                scanFile(outputFile);
                setLastRecorderFile(outputFile.getAbsolutePath());
                notificationSaved();

                mWatermark.stop();
                showRecorderOverlay();
                mNativeProcessRunner.initialize();
            }
        });
    }

    private void scanFile(File file) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.TITLE, file.getName());
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.DATA,file.getAbsolutePath());

        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private void notificationSaved() {
        String message = String.format(getString(R.string.recording_saved_message), outputFile.getName());
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.recording_saved_title))
                        .setContentText(message);

        Intent playVideoIntent = getPlayVideoIntent();
        Intent recorderIntent = new Intent(this, RecorderActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivities(this, 0, new Intent[]{recorderIntent, playVideoIntent}, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
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
                mWatermark.stop();
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
                mWatermark.stop();
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
                mWatermark.stop();
                showRecorderOverlay();
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRecording) {
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }
        mWatermark.hide();
        mRecorderOverlay.hide();
        mScreenOffReceiver.unregister();
        savePreferences();
    }

    private void setLastRecorderFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                mLastRecorderFile = path;
            } else {
                mLastRecorderFile = null;
            }
        } else {
            mLastRecorderFile = null;
        }

    }

    private void readPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        setLastRecorderFile(preferences.getString(LAST_RECORDED_FILE_PREFERENCE, null));

    }

    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LAST_RECORDED_FILE_PREFERENCE, mLastRecorderFile);
        editor.commit();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
