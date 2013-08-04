package com.iwobanas.screenrecorder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.iwobanas.screenrecorder.settings.Settings;
import com.iwobanas.screenrecorder.settings.SettingsActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.iwobanas.screenrecorder.Tracker.*;

public class RecorderService extends Service implements IRecorderService, LicenseCheckerCallback {

    public static final String STOP_HELP_DISPLAYED_EXTRA = "STOP_HELP_DISPLAYED_EXTRA";

    public static final String SETTINGS_CLOSED_EXTRA = "SETTINGS_CLOSED_EXTRA";

    public static final String TIMEOUT_DIALOG_CLOSED_EXTRA = "TIMEOUT_DIALOG_CLOSED_EXTRA";

    public static final String HIDE_ICON_DIALOG_CLOSED_EXTRA = "HIDE_ICON_DIALOG_CLOSED_EXTRA";

    private static final String TAG = "scr_RecorderService";

    public static final String PREFERENCES_NAME = "ScreenRecorderPreferences";

    private static final String STOP_HELP_DISPLAYED_PREFERENCE = "stopHelpDisplayed";

    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private WatermarkOverlay mWatermark = new WatermarkOverlay(this);

    private IScreenOverlay mRecorderOverlay = new RecorderOverlay(this, this);

    private ScreenOffReceiver mScreenOffReceiver = new ScreenOffReceiver(this, this);

    private NativeProcessRunner mNativeProcessRunner = new NativeProcessRunner(this);

    private RecordingTimeController mTimeController = new RecordingTimeController(this);

    private Handler mHandler;

    private File outputFile;

    private boolean isRecording;

    private boolean isReady;

    private boolean isTimeoutDisplayed;

    private long mRecordingStartTime;

    private boolean mTaniosc = true;

    // Preferences
    private boolean mStopHelpDisplayed;

    @Override
    public void onCreate() {
        Settings.initialize(this);
        mTaniosc = getResources().getBoolean(R.bool.taniosc);
        mHandler = new Handler();
        mWatermark.show();

        readPreferences();
        installExecutable();

        EasyTracker.getInstance().setContext(getApplicationContext());

        mRecorderOverlay.show();
        isReady = false;
        mNativeProcessRunner.initialize();

        if (!mTaniosc) {
            checkLicense();
        }
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
            EasyTracker.getTracker().sendEvent(ERROR, INSTALLATION_ERROR, INSTALLATION_ERROR, null);
            EasyTracker.getTracker().sendException(Thread.currentThread().getName(), e, false);
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
        if (!isReady) {
            return;
            //TODO: indicate to the user that recorder is not ready e.g. grey out button
        }
        if (!mStopHelpDisplayed) {
            mRecorderOverlay.hide();
            displayStopHelp();
            return;
        }
        mRecorderOverlay.hide();
        if (mTaniosc) {
            mWatermark.start();
            mTimeController.start();
        } else {
            mWatermark.hide();
        }
        mScreenOffReceiver.register();
        outputFile = getOutputFile();
        isRecording = true;
        mNativeProcessRunner.start(outputFile.getAbsolutePath(), getRotation());
        mRecordingStartTime = System.currentTimeMillis();

        EasyTracker.getTracker().sendEvent(ACTION, START, START, null);
        EasyTracker.getTracker().sendEvent(SETTINGS, AUDIO, Settings.getInstance().getAudioSource().name(), null);
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
        rotationDeg = (360 - rotationDeg) % 360;
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

    @Override
    public void stopRecording() {
        isRecording = false;
        mNativeProcessRunner.stop();
        mTimeController.reset();
    }

    private Intent getPlayVideoIntent() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(outputFile), "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public void close() {
        stopSelf();
    }

    @Override
    public void setReady(boolean ready) {
        isReady = true;
    }

    private void showRecorderOverlay() {
        if (!isTimeoutDisplayed) {
            mRecorderOverlay.show();
        }
        mScreenOffReceiver.unregister();

    }

    @Override
    public void recordingFinished() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                scanOutputAndNotify();

                if (mTaniosc) {
                    mWatermark.stop();
                } else {
                    mWatermark.show();
                }
                mTimeController.reset();
                showRecorderOverlay();
                isReady = false;
                mNativeProcessRunner.initialize();
            }
        });
    }

    public void scanOutputAndNotify() {
        String message = String.format(getString(R.string.recording_saved_toast), outputFile.getName());
        Toast.makeText(RecorderService.this, message, Toast.LENGTH_LONG).show();
        scanFile(outputFile);
        notificationSaved();

        EasyTracker.getTracker().sendEvent(STATS, RECORDING, SIZE, outputFile.length() / 1000000l);
        EasyTracker.getTracker().sendEvent(STATS, RECORDING, TIME, (System.currentTimeMillis() - mRecordingStartTime) / 1000l);
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
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
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

    private void displayStopHelp() {
        Intent intent = new Intent(RecorderService.this, DialogActivity.class);
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.help_stop_message));
        intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.help_stop_title));
        intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.help_stop_ok));
        intent.putExtra(DialogActivity.RESTART_EXTRA, true);
        intent.putExtra(DialogActivity.RESTART_EXTRA_EXTRA, STOP_HELP_DISPLAYED_EXTRA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        mStopHelpDisplayed = true;
    }

    private void startForeground() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.app_full_name));

        if (!mTaniosc && Settings.getInstance().getHideIcon()) {
            builder.setSmallIcon(R.drawable.transparent);
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        } else {
            builder.setSmallIcon(R.drawable.ic_notification);
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        PendingIntent intent = PendingIntent.getService(this, 0, new Intent(this, RecorderService.class), 0);
        builder.setContentIntent(intent);

        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    @Override
    public void suRequired() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = getString(R.string.su_required_message);
                String title = getString(R.string.su_required_title);
                displayErrorMessage(message, title, false, false, 0);
            }
        });
    }

    private void displayErrorMessage(final String message, final String title, final boolean restart, boolean report, int errorCode) {
        Intent intent = new Intent(RecorderService.this, DialogActivity.class);
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
        intent.putExtra(DialogActivity.TITLE_EXTRA, title);
        intent.putExtra(DialogActivity.RESTART_EXTRA, restart);
        intent.putExtra(DialogActivity.REPORT_BUG_EXTRA, report);
        intent.putExtra(DialogActivity.REPORT_BUG_ERROR_EXTRA, errorCode);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Log.w(TAG, "displayErrorMessage: " + message);
        stopSelf();
    }

    @Override
    public void startupError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.startup_error_message), exitValue);
                displayErrorMessage(message, getString(R.string.error_dialog_title), false, true, exitValue);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, STARTUP_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void recordingError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.recording_error_message), exitValue);
                displayErrorMessage(message, getString(R.string.error_dialog_title), true, true, exitValue);
                if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                    scanOutputAndNotify();
                }
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void mediaRecorderError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = getString(R.string.media_recorder_error_message);
                displayErrorMessage(message, getString(R.string.media_recorder_error_title), false, true, exitValue);
                if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                    scanOutputAndNotify();
                }
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void showSettings() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mRecorderOverlay.hide();
                Intent intent = new Intent(RecorderService.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
    }

    @Override
    public void showTimeoutDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                isTimeoutDisplayed = true;
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.free_timeout_message));
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.free_timeout_title));
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.free_timeout_buy));
                intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.free_timeout_no_thanks));
                intent.putExtra(DialogActivity.RESTART_EXTRA, true);
                intent.putExtra(DialogActivity.RESTART_EXTRA_EXTRA, TIMEOUT_DIALOG_CLOSED_EXTRA);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    @Override
    public String getDeviceId() {
        return Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    }

    private void buyPro() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.iwobanas.screenrecorder.pro"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            EasyTracker.getTracker().sendEvent(ERROR, BUY_ERROR, TIMEOUT_DIALOG, null);
            displayErrorMessage(getString(R.string.buy_error_message), getString(R.string.buy_error_title), true, true, -1);
        }
        EasyTracker.getTracker().sendEvent(ACTION, BUY, TIMEOUT_DIALOG, null);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        if (intent.getBooleanExtra(STOP_HELP_DISPLAYED_EXTRA, false)) {
            startRecording();
        } else if (intent.getBooleanExtra(SETTINGS_CLOSED_EXTRA, false)) {
            mRecorderOverlay.show();
            //refresh settings if needed
        } else if (intent.getBooleanExtra(TIMEOUT_DIALOG_CLOSED_EXTRA, false)) {
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                buyPro();
            } else {
                isTimeoutDisplayed = false;
                mRecorderOverlay.show();
            }
        } else if (intent.getBooleanExtra(HIDE_ICON_DIALOG_CLOSED_EXTRA, false)) {
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                buyPro();
            } else {
                mRecorderOverlay.show();
            }
        } else if (isRecording) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_ICON, null);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_DESTROY, null);
        }
        mWatermark.hide();
        mRecorderOverlay.hide();
        mNativeProcessRunner.destroy();
        mScreenOffReceiver.unregister();
        savePreferences();
    }

    private void readPreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        mStopHelpDisplayed = preferences.getBoolean(STOP_HELP_DISPLAYED_PREFERENCE, false);
    }

    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(STOP_HELP_DISPLAYED_PREFERENCE, mStopHelpDisplayed);
        editor.commit();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Licensing
    private static final byte[] LICENSE_SALT = new byte[]{ 95, -9, 7, -80, -79, -72, 3, -116, 95, 79, -18, 63, -124, -85, -71, -2, -73, -37, 47,  122};
    private static final String LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmOZqTyb4AOB4IEWZiXd0SRYyJ2Y0xu1FBDmxvQqFG+D1wMJMKPxJlMNYwwS3AYjGgzhJzdWFd+oMaRV5uD9BWinHXyUppIrQcHfINv1J9VuwQnVQVYDG+EEiKOAGnnOLhg5EaJ5bdpvRyMLpD3wz9qcIx1YC99/TJC+ACABrhCfkc+U9hKyNe0m4C7DHBEW4SIq22bC1vPOw5KgbdruFxRoQiYU3GE7o8/fH37Vk9Rc+75QrtNYsJ9W0Vm7f2brN+lVwnQVEfsRVBr4k+yHVDVdo82SQfiUo6Q6d0S3HMCqMeRe8UQxGpPxRpE75cADR3LyyduRJ4+KJHPuY38AEAQIDAQAB";
    private LicenseChecker mChecker;

    private void checkLicense() {
        String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(LICENSE_SALT, getPackageName(), deviceId)),
                LICENSE_KEY);

        mChecker.checkAccess(this);
    }

    @Override
    public void allow(int policyReason) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ALLOW_ + policyReason, null);
    }

    @Override
    public void dontAllow(int policyReason) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_DONT_ALLOW_ + policyReason, null);
        mTaniosc = true;
        if (isRecording) {
            mWatermark.show();
            mWatermark.start();
        }
        Toast.makeText(this, getString(R.string.license_dont_allow), Toast.LENGTH_LONG).show();
    }

    @Override
    public void applicationError(int errorCode) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ERROR_ + errorCode, null);
        mTaniosc = true;
        if (isRecording) {
            mWatermark.show();
            mWatermark.start();
        }
        Toast.makeText(this, getString(R.string.license_error), Toast.LENGTH_LONG).show();
    }
}
