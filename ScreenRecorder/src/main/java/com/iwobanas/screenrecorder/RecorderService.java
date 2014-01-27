package com.iwobanas.screenrecorder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
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
import com.iwobanas.screenrecorder.rating.RatingController;
import com.iwobanas.screenrecorder.settings.Settings;
import com.iwobanas.screenrecorder.settings.SettingsActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.iwobanas.screenrecorder.Tracker.ACTION;
import static com.iwobanas.screenrecorder.Tracker.AUDIO;
import static com.iwobanas.screenrecorder.Tracker.BUY;
import static com.iwobanas.screenrecorder.Tracker.BUY_ERROR;
import static com.iwobanas.screenrecorder.Tracker.ERROR;
import static com.iwobanas.screenrecorder.Tracker.ERROR_;
import static com.iwobanas.screenrecorder.Tracker.LICENSE;
import static com.iwobanas.screenrecorder.Tracker.LICENSE_ALLOW_;
import static com.iwobanas.screenrecorder.Tracker.LICENSE_DONT_ALLOW_;
import static com.iwobanas.screenrecorder.Tracker.LICENSE_ERROR_;
import static com.iwobanas.screenrecorder.Tracker.RECORDING;
import static com.iwobanas.screenrecorder.Tracker.RECORDING_ERROR;
import static com.iwobanas.screenrecorder.Tracker.SETTINGS;
import static com.iwobanas.screenrecorder.Tracker.SIZE;
import static com.iwobanas.screenrecorder.Tracker.START;
import static com.iwobanas.screenrecorder.Tracker.STARTUP_ERROR;
import static com.iwobanas.screenrecorder.Tracker.STATS;
import static com.iwobanas.screenrecorder.Tracker.STOP;
import static com.iwobanas.screenrecorder.Tracker.STOP_DESTROY;
import static com.iwobanas.screenrecorder.Tracker.STOP_ICON;
import static com.iwobanas.screenrecorder.Tracker.TIME;
import static com.iwobanas.screenrecorder.Tracker.TIMEOUT_DIALOG;

public class RecorderService extends Service implements IRecorderService, LicenseCheckerCallback {

    public static final String STOP_HELP_DISPLAYED_EXTRA = "STOP_HELP_DISPLAYED_EXTRA";
    public static final String TIMEOUT_DIALOG_CLOSED_EXTRA = "TIMEOUT_DIALOG_CLOSED_EXTRA";
    public static final String RESTART_MUTE_EXTRA = "RESTART_MUTE_EXTRA";
    public static final String PLAY_ACTION = "scr.intent.action.PLAY";
    public static final String PREFERENCES_NAME = "ScreenRecorderPreferences";
    public static final String START_RECORDING_ACTION = "scr.intent.action.START_RECORDING";
    private static final String TAG = "scr_RecorderService";
    private static final String STOP_HELP_DISPLAYED_PREFERENCE = "stopHelpDisplayed";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    // Licensing
    private static final byte[] LICENSE_SALT = new byte[]{95, -9, 7, -80, -79, -72, 3, -116, 95, 79, -18, 63, -124, -85, -71, -2, -73, -37, 47, 122};
    private static final String LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmOZqTyb4AOB4IEWZiXd0SRYyJ2Y0xu1FBDmxvQqFG+D1wMJMKPxJlMNYwwS3AYjGgzhJzdWFd+oMaRV5uD9BWinHXyUppIrQcHfINv1J9VuwQnVQVYDG+EEiKOAGnnOLhg5EaJ5bdpvRyMLpD3wz9qcIx1YC99/TJC+ACABrhCfkc+U9hKyNe0m4C7DHBEW4SIq22bC1vPOw5KgbdruFxRoQiYU3GE7o8/fH37Vk9Rc+75QrtNYsJ9W0Vm7f2brN+lVwnQVEfsRVBr4k+yHVDVdo82SQfiUo6Q6d0S3HMCqMeRe8UQxGpPxRpE75cADR3LyyduRJ4+KJHPuY38AEAQIDAQAB";
    private WatermarkOverlay mWatermark = new WatermarkOverlay(this);
    private IScreenOverlay mRecorderOverlay = new RecorderOverlay(this, this);
    private ScreenOffReceiver mScreenOffReceiver = new ScreenOffReceiver(this, this);
    private NativeProcessRunner mNativeProcessRunner = new NativeProcessRunner(this);
    private RecordingTimeController mTimeController = new RecordingTimeController(this);
    private RatingController mRatingController;
    private Handler mHandler;
    private File outputFile;
    private RecorderServiceState state = RecorderServiceState.INSTALLING;
    private boolean isTimeoutDisplayed;
    private boolean startOnReady;
    private long mRecordingStartTime;
    private boolean mTaniosc = true;
    // Preferences
    private boolean mStopHelpDisplayed;
    private LicenseChecker mChecker;

    @Override
    public void onCreate() {
        EasyTracker.getInstance().setContext(getApplicationContext());

        if (Build.VERSION.SDK_INT < 15 || Build.VERSION.SDK_INT > 19) {
            displayErrorMessage(getString(R.string.android_version_error_message), getString(R.string.android_version_error_title), false, false, -1);
        }

        Settings.initialize(this);
        mTaniosc = getResources().getBoolean(R.bool.taniosc);
        mHandler = new Handler();
        mWatermark.show();

        mRatingController = new RatingController(this);
        if (mRatingController.shouldShow()) {
            mRatingController.show();
            stopSelf();
        }

        readPreferences();
        installExecutable();

        mRecorderOverlay.show();
        reinitialize();

        if (!mTaniosc) {
            checkLicense();
        }
        Log.v(TAG, "Service initialized. version: " + Utils.getAppVersion(this));
    }

    private void installExecutable() {
        new InstallExecutableAsyncTask(this, this).execute();
    }

    public void executableInstalled(String executable) {
        setState(RecorderServiceState.INITIALIZING);
        mNativeProcessRunner.initialize(executable);
    }

    @Override
    public void recordingStarted() {
        setState(RecorderServiceState.RECORDING);
    }

    private void setState(RecorderServiceState state) {
        this.state = state;
        startForeground();
    }

    @Override
    public void startRecording() {
        if (state != RecorderServiceState.READY) {
            return;
            //TODO: indicate to the user that recorder is not ready e.g. grey out button
        }
        if (!mStopHelpDisplayed) {
            mRecorderOverlay.hide();
            displayStopHelp();
            return;
        }
        setState(RecorderServiceState.STARTING);
        mRecorderOverlay.hide();
        if (mTaniosc) {
            mWatermark.start();
            mTimeController.start();
        } else {
            mWatermark.hide();
        }
        if (Settings.getInstance().getStopOnScreenOff()) {
            mScreenOffReceiver.register();
        }
        outputFile = getOutputFile();
        mNativeProcessRunner.start(outputFile.getAbsolutePath(), getRotation());
        mRecordingStartTime = System.currentTimeMillis();

        EasyTracker.getTracker().sendEvent(ACTION, START, START, null);
        EasyTracker.getTracker().sendEvent(SETTINGS, AUDIO, Settings.getInstance().getAudioSource().name(), null);
    }

    private synchronized void startRecordingWhenReady() {
        if (state == RecorderServiceState.READY) {
            startRecording();
        } else {
            startOnReady = true;
        }
    }

    private File getOutputFile() {
        //TODO: check external storage state
        File dir = Settings.getInstance().getOutputDir();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.w(TAG, "mkdirs failed " + dir.getAbsolutePath() + " fallback to legacy storage dir");
                // fallback to legacy path /sdcard
                dir = new File("/sdcard", getString(R.string.output_dir));
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        Log.e(TAG, "mkdirs failed " + dir.getAbsolutePath());
                        //TODO: display error message
                    }
                }
            }
        }
        SimpleDateFormat format = new SimpleDateFormat(getString(R.string.file_name_format));
        return new File(dir, format.format(new Date()));
    }

    private String getRotation() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
        setState(RecorderServiceState.STOPPING);
        mNativeProcessRunner.stop();
        mTimeController.reset();
    }

    private void playVideo(Uri uri) {
        mRecorderOverlay.hide();
        mWatermark.hide();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void close() {
        stopSelf();
    }

    @Override
    public synchronized void setReady() {
        setState(RecorderServiceState.READY);
        Settings.getInstance().applyShowTouches();
        if (startOnReady) {
            startOnReady = false;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    startRecording();
                }
            });
        }
    }

    private void showRecorderOverlay() {
        if (!isTimeoutDisplayed) {
            mRecorderOverlay.show();
        }
        mScreenOffReceiver.unregister();

    }

    @Override
    public void recordingFinished(final float fps) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                scanOutputAndNotify(R.string.recording_saved_toast);
                reportRecordingStats(-1, fps);
                reinitializeView();
                reinitialize();
                mRatingController.increaseSuccessCount();
            }
        });
    }

    private synchronized void reinitializeView() {
        if (mTaniosc) {
            mWatermark.stop();
        } else {
            mWatermark.show();
        }
        showRecorderOverlay();
    }

    private void reinitialize() {
        mTimeController.reset();

        if (state != RecorderServiceState.INSTALLING) {
            setState(RecorderServiceState.INITIALIZING);
        }
        mNativeProcessRunner.initialize();
    }

    private void reportRecordingStats(int errorCode, float fps) {
        long sizeK = outputFile.length() / 1024l;
        long sizeM = sizeK / 1024l;
        long time = (System.currentTimeMillis() - mRecordingStartTime) / 1000l;
        EasyTracker.getTracker().sendEvent(STATS, RECORDING, SIZE, sizeM);
        EasyTracker.getTracker().sendEvent(STATS, RECORDING, TIME, time);
        logStats(errorCode, (int) sizeK, (int) time, fps);
    }

    public void scanOutputAndNotify(int toastId) {
        String message = String.format(getString(toastId), outputFile.getName());
        Toast.makeText(RecorderService.this, message, Toast.LENGTH_LONG).show();
        scanFile(outputFile);
        notificationSaved();
    }

    private void logStats(int exitValue, int size, int time) {
        logStats(exitValue, size, time, -1);
    }

    private void logStats(int exitValue, int size, int time, float fps) {
        int appVersion = Utils.getAppVersion(this);
        new SendStatsAsyncTask(getPackageName(), appVersion, getDeviceId(), outputFile.getName(), exitValue, size, time, fps).execute();
    }

    private void scanFile(File file) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.TITLE, file.getName());
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, mRecordingStartTime);
        contentValues.put(MediaStore.Video.Media.DATE_ADDED, mRecordingStartTime / 1000);
        contentValues.put(MediaStore.Video.Media.DATE_MODIFIED, mRecordingStartTime / 1000);

        getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private void notificationSaved() {
        String message = String.format(getString(R.string.recording_saved_message), outputFile.getName());
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification_saved)
                        .setContentTitle(getString(R.string.recording_saved_title))
                        .setContentText(message);

        Intent playIntent = new Intent(this, RecorderService.class);
        playIntent.setAction(PLAY_ACTION);
        playIntent.setData(Uri.fromFile(outputFile));
        mBuilder.setContentIntent(PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_ONE_SHOT));
        mBuilder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, mBuilder.build());
    }

    private void displayStopHelp() {
        Intent intent = new Intent(RecorderService.this, DialogActivity.class);
        String message = String.format(getString(R.string.help_stop_message), getString(R.string.app_name));
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
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
        builder.setWhen(0);
        builder.setContentText(getStatusString());

        if (!mTaniosc && Settings.getInstance().getHideIcon()) {
            builder.setSmallIcon(R.drawable.transparent);
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        } else {
            builder.setSmallIcon(R.drawable.ic_notification);
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        }

        PendingIntent intent = PendingIntent.getService(this, 0, new Intent(this, RecorderService.class), PendingIntent.FLAG_ONE_SHOT);
        builder.setContentIntent(intent);

        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private CharSequence getStatusString() {
        switch (state) {
            case INITIALIZING:
                return getString(R.string.notification_status_initializing);
            case INSTALLING:
                return getString(R.string.notification_status_installing);
            case READY:
                return getString(R.string.notification_status_ready);
            case STARTING:
                return getString(R.string.notification_status_starting);
            case RECORDING:
                return getString(R.string.notification_status_recording);
            case STOPPING:
                return getString(R.string.notification_status_stopping);
        }
        return "";
    }

    @Override
    public void suRequired() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.su_required_message));
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.su_required_title));
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.su_required_help));
                Intent helpIntent = new Intent(Intent.ACTION_VIEW);
                helpIntent.setData(Uri.parse(getString(R.string.su_required_help_link)));
                helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, helpIntent);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                stopSelf();
            }
        });
    }

    @Override
    public void cpuNotSupportedError() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.cpu_error_message), Build.CPU_ABI, getString(R.string.app_name));
                displayErrorMessage(message, getString(R.string.cpu_error_title), false, false, -1);
            }
        });
    }

    @Override
    public void installationError() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.installation_error_message), getString(R.string.app_name));
                displayErrorMessage(message, getString(R.string.installation_error_title), false, false, -1);
            }
        });
    }

    //TODO: review when service should really be restarted

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
        if (mRatingController != null) {
            mRatingController.resetSuccessCount();
        }
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
                    scanOutputAndNotify(R.string.recording_saved_toast);
                }
                logStats(exitValue, 0, 0);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void mediaRecorderError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.media_recorder_error_message), exitValue);
                displayErrorMessage(message, getString(R.string.media_recorder_error_title), true, true, exitValue);
                if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                    scanOutputAndNotify(R.string.recording_saved_toast);
                }
                logStats(exitValue, 0, 0);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void maxFileSizeReached(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                scanOutputAndNotify(R.string.max_file_size_reached_toast);
                reportRecordingStats(exitValue, -1.0f);
                reinitializeView();
                mRatingController.increaseSuccessCount();
                reinitialize();
            }
        });
    }

    @Override
    public void outputFileError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.output_file_error_message), outputFile);
                displayErrorMessage(message, getString(R.string.output_file_error_title), true, false, exitValue);
                logStats(exitValue, 0, 0);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void microphoneBusyError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.microphone_busy_error_message));
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.microphone_busy_error_title));
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.microphone_busy_error_continue_mute));
                intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.settings_cancel));
                intent.putExtra(DialogActivity.RESTART_EXTRA, true);
                intent.putExtra(DialogActivity.RESTART_EXTRA_EXTRA, RESTART_MUTE_EXTRA);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                logStats(exitValue, 0, 0);
                reinitialize();
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void openGlError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                displayErrorMessage(getString(R.string.opengl_error_message), getString(R.string.opengl_error_title), true, true, exitValue);
                logStats(exitValue, 0, 0);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void secureSurfaceError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                displayErrorMessage(getString(R.string.screen_protected_error_message), getString(R.string.screen_protected_error_title), true, false, exitValue);
                logStats(exitValue, 0, 0);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + exitValue, null);
    }

    @Override
    public void audioConfigError(final int exitValue) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                displayErrorMessage(getString(R.string.audio_config_error_message), getString(R.string.audio_config_error_title), true, false, exitValue);
                logStats(exitValue, 0, 0);
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
                mWatermark.hide();

                Intent intent = new Intent(RecorderService.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                mRatingController.resetSuccessCount();
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
        } else if (intent.getBooleanExtra(TIMEOUT_DIALOG_CLOSED_EXTRA, false)) {
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                buyPro();
            } else {
                isTimeoutDisplayed = false;
                mRecorderOverlay.show();
            }
        } else if (intent.getBooleanExtra(RESTART_MUTE_EXTRA,false)) {
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                Settings.getInstance().setTemporaryMute(true);
                startRecordingWhenReady();
            } else {
                reinitializeView();
            }
        } else if (PLAY_ACTION.equals(intent.getAction())) {
            playVideo(intent.getData());
        } else if (START_RECORDING_ACTION.equals(intent.getAction())) {
            startRecordingWhenReady();
        } else if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_ICON, null);
        } else {
            mRecorderOverlay.show();
            mWatermark.show();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_DESTROY, null);
        }
        startOnReady = false;
        mWatermark.hide();
        mWatermark.onDestroy();
        mRecorderOverlay.hide();
        mRecorderOverlay.onDestroy();
        mNativeProcessRunner.destroy();
        mScreenOffReceiver.unregister();
        savePreferences();
        Settings.getInstance().restoreShowTouches();
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
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            mWatermark.show();
            mWatermark.start();
        }
        Toast.makeText(this, getString(R.string.license_dont_allow), Toast.LENGTH_LONG).show();
    }

    @Override
    public void applicationError(int errorCode) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ERROR_ + errorCode, null);
        mTaniosc = true;
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            mWatermark.show();
            mWatermark.start();
        }
        Toast.makeText(this, getString(R.string.license_error), Toast.LENGTH_LONG).show();
    }

    private static enum RecorderServiceState {
        INSTALLING,
        INITIALIZING,
        READY,
        STARTING,
        RECORDING,
        STOPPING
    }
}
