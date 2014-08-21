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
import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.iwobanas.screenrecorder.audio.AudioDriver;
import com.iwobanas.screenrecorder.audio.InstallationStatus;
import com.iwobanas.screenrecorder.rating.RatingController;
import com.iwobanas.screenrecorder.settings.AudioSource;
import com.iwobanas.screenrecorder.settings.Settings;
import com.iwobanas.screenrecorder.settings.SettingsActivity;
import com.iwobanas.screenrecorder.stats.RecordingStatsAsyncTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

public class RecorderService extends Service implements IRecorderService, LicenseCheckerCallback, AudioDriver.OnInstallListener {

    public static final String STOP_HELP_DISPLAYED_ACTION = "scr.intent.action.STOP_HELP_DISPLAYED";
    public static final String TIMEOUT_DIALOG_CLOSED_ACTION = "scr.intent.action.TIMEOUT_DIALOG_CLOSED";
    public static final String LICENSE_DIALOG_CLOSED = "scr.intent.action.LICENSE_DIALOG_CLOSED";
    public static final String RESTART_MUTE_ACTION = "scr.intent.action.RESTART_MUTE";
    public static final String PLAY_ACTION = "scr.intent.action.PLAY";
    public static final String PREFERENCES_NAME = "ScreenRecorderPreferences";
    public static final String START_RECORDING_ACTION = "scr.intent.action.START_RECORDING";
    public static final String DIALOG_CLOSED_ACTION = "scr.intent.action.DIALOG_CLOSED";
    public static final String RATING_DIALOG_CLOSED_ACTION = "scr.intent.action.RATING_DIALOG_CLOSED";
    public static final String SETTINGS_CLOSED_ACTION = "scr.intent.action.SETTINGS_CLOSED";
    public static final String SETTINGS_OPENED_ACTION = "scr.intent.action.SETTINGS_OPENED";
    public static final String ERROR_DIALOG_CLOSED_ACTION = "scr.intent.action.ERROR_DIALOG_CLOSED";
    public static final String NOTIFICATION_ACTION = "scr.intent.action.NOTIFICATION";
    public static final String LOUNCHER_ACTION = "scr.intent.action.LOUNCHER";
    private static final String TAG = "scr_RecorderService";
    private static final String STOP_HELP_DISPLAYED_PREFERENCE = "stopHelpDisplayed";
    private static final String SHUT_DOWN_CORRECTLY = "SHUT_DOWN_CORRECTLY";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    // Licensing
    private static final byte[] LICENSE_SALT = new byte[]{95, -9, 7, -80, -79, -72, 3, -116, 95, 79, -18, 63, -124, -85, -71, -2, -73, -37, 47, 122};
    private static final String LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmOZqTyb4AOB4IEWZiXd0SRYyJ2Y0xu1FBDmxvQqFG+D1wMJMKPxJlMNYwwS3AYjGgzhJzdWFd+oMaRV5uD9BWinHXyUppIrQcHfINv1J9VuwQnVQVYDG+EEiKOAGnnOLhg5EaJ5bdpvRyMLpD3wz9qcIx1YC99/TJC+ACABrhCfkc+U9hKyNe0m4C7DHBEW4SIq22bC1vPOw5KgbdruFxRoQiYU3GE7o8/fH37Vk9Rc+75QrtNYsJ9W0Vm7f2brN+lVwnQVEfsRVBr4k+yHVDVdo82SQfiUo6Q6d0S3HMCqMeRe8UQxGpPxRpE75cADR3LyyduRJ4+KJHPuY38AEAQIDAQAB";
    private IScreenOverlay mWatermark = new WatermarkOverlay(this);
    private RecorderOverlay mRecorderOverlay = new RecorderOverlay(this, this);
    private CameraOverlay mCameraOverlay;
    private ScreenOffReceiver mScreenOffReceiver = new ScreenOffReceiver(this, this);
    private NativeProcessRunner mNativeProcessRunner = new NativeProcessRunner(this, this);
    private RecordingTimeController mTimeController = new RecordingTimeController(this);
    private RatingController mRatingController;
    private AudioDriver audioDriver;
    private Handler mHandler;
    private File outputFile;
    private RecorderServiceState state = RecorderServiceState.INSTALLING;
    private boolean isTimeoutDisplayed;
    private boolean startOnReady;
    private long mRecordingStartTime;
    private boolean mTaniosc = true;
    private boolean retryLicenseCheck = false;
    private boolean firstCommand = true;
    private boolean closing = false;
    private boolean destroyed = false;
    private boolean settingsDisplayed = false;
    private boolean displayShutDownError = false;
    private Toast cantStartToast;

    // Preferences
    private boolean mStopHelpDisplayed;
    private LicenseChecker mChecker;

    @Override
    public void onCreate() {
        EasyTracker.getInstance().setContext(getApplicationContext());
        initializeExceptionParser();
        mHandler = new Handler();

        if (Build.VERSION.SDK_INT < 15 || Build.VERSION.SDK_INT > 19) {
            displayErrorMessage(getString(R.string.android_version_error_message), getString(R.string.android_version_error_title), false, false, -1);
        }

        Settings.initialize(this);
        audioDriver = Settings.getInstance().getAudioDriver();
        audioDriver.addInstallListener(this);
        mTaniosc = getResources().getBoolean(R.bool.taniosc);
        checkShutDownCorrectly();

        mCameraOverlay = new CameraOverlay(this);
        mCameraOverlay.applySettings();

        mRatingController = new RatingController(this);

        readPreferences();
        installExecutable();

        mRecorderOverlay.animateShow();
        reinitialize();

        if (!mTaniosc) {
            checkLicense();
        }
        Log.v(TAG, "Service initialized. version: " + Utils.getAppVersion(this));
    }

    private void initializeExceptionParser() {
        List<String> packages = Arrays.asList( "com.iwobanas", "com.google.android.vending");
        ExceptionParser exceptionParser = new AnalyticsExceptionParser(getApplicationContext(), packages);
        EasyTracker.getTracker().setExceptionParser(exceptionParser);
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (uncaughtExceptionHandler instanceof ExceptionReporter) {
            ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
            exceptionReporter.setExceptionParser(exceptionParser);
        }
    }

    private void installExecutable() {
        new InstallExecutableAsyncTask(this, this).execute();
    }

    public void executableInstalled(final String executable) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (destroyed) return;
                setState(RecorderServiceState.INITIALIZING);
                mNativeProcessRunner.initialize(executable);
            }
        });
    }

    @Override
    public void recordingStarted() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setState(RecorderServiceState.RECORDING);
            }
        });
    }

    private void setState(RecorderServiceState state) {
        this.state = state;
        if (!destroyed) {
            startForeground();
        }
    }

    @Override
    public void startRecording() {
        if (cantStartToast != null) {
            cantStartToast.cancel();
        }
        if (state != RecorderServiceState.READY) {
            cantStartToast = Toast.makeText(this, getString(R.string.can_not_start_toast, getStatusString()), Toast.LENGTH_SHORT);
            cantStartToast.show();
            return;
        }
        if (!mStopHelpDisplayed) {
            mRecorderOverlay.hide();
            displayStopHelp();
            return;
        }
        setState(RecorderServiceState.STARTING);
        mRecorderOverlay.hide();
        mCameraOverlay.setTouchable(false);
        if (mTaniosc) {
            mWatermark.show();
            mTimeController.start();
        }
        Settings.getInstance().applyShowTouches();
        if (Settings.getInstance().getStopOnScreenOff()) {
            mScreenOffReceiver.register();
        }
        audioDriver.startRecording();
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
        mCameraOverlay.setTouchable(true);
    }

    private void playVideo(Uri uri) {
        mRecorderOverlay.hide();
        mCameraOverlay.hide();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Couldn't play video file, attempting with different mime type", e);
            intent.setDataAndType(uri, "video/mp4");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ee) {
                Log.e(TAG, "Couldn't play video file", ee);
            }
        }
    }

    @Override
    public void close() {
        closing = true;
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            stopRecording();
        }
        startOnReady = false;
        mRecorderOverlay.animateHide();
        mCameraOverlay.hide();
        if (audioDriver.shouldUninstall()) {
            audioDriver.uninstall();
        } else {
            stopSelf();
        }
    }

    @Override
    public synchronized void setReady() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                checkReady();
            }
        });
    }

    private void checkReady() {
        if (mNativeProcessRunner.isReady() && audioDriver.isReady()) {
            setState(RecorderServiceState.READY);
            if (startOnReady) {
                startOnReady = false;

                startRecording();

            }
        } else if (audioDriver.shouldInstall()) {
            audioDriver.install();
        }
    }

    @Override
    public void recordingFinished(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                scanOutputAndNotify(R.string.recording_saved_toast);
                reportRecordingStats(recordingInfo);
                reinitializeView();
                reinitialize();
                mRatingController.increaseSuccessCount();
            }
        });
    }

    private synchronized void reinitializeView() {
        if (mTaniosc) {
            mWatermark.hide();
        }
        Settings.getInstance().restoreShowTouches();
        if (!isTimeoutDisplayed) {
            mRecorderOverlay.animateShow();
        }
        mCameraOverlay.setTouchable(true);
    }

    private void reinitialize() {
        mTimeController.reset();
        mScreenOffReceiver.unregister();

        if (state != RecorderServiceState.INSTALLING) {
            setState(RecorderServiceState.INITIALIZING);
        }
        mNativeProcessRunner.initialize();
    }

    private void reportRecordingStats(RecordingInfo recordingInfo) {
        logStats(recordingInfo);

        long sizeM = recordingInfo.size / 1024l;
        EasyTracker.getTracker().sendEvent(STATS, RECORDING, SIZE, sizeM);
        EasyTracker.getTracker().sendEvent(STATS, RECORDING, TIME, (long) recordingInfo.time);
    }

    public void scanOutputAndNotify(int toastId) {
        String message = String.format(getString(toastId), outputFile.getName());
        Toast.makeText(RecorderService.this, message, Toast.LENGTH_LONG).show();
        scanFile(outputFile);
        notificationSaved();
    }

    private void logStats(RecordingInfo recordingInfo) {
        recordingInfo.size = (int) (outputFile.length() / 1024l);
        recordingInfo.time = (int) ((System.currentTimeMillis() - mRecordingStartTime) / 1000l);
        new RecordingStatsAsyncTask(this, recordingInfo).execute();
        if (Settings.getInstance().getAudioSource() == AudioSource.INTERNAL) {
            audioDriver.logStats(recordingInfo);
        }
    }

    private void scanFile(File file) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.TITLE, file.getName());
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, mRecordingStartTime);
        contentValues.put(MediaStore.Video.Media.DATE_ADDED, mRecordingStartTime / 1000);
        contentValues.put(MediaStore.Video.Media.DATE_MODIFIED, mRecordingStartTime / 1000);

        try {
            getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting video content values", e);
        }
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

        try {
            mNotificationManager.notify(0, mBuilder.build());
        } catch (SecurityException e) {
            // Android 4.1.2 issue
            // could be fixed by adding <uses-permission android:name="android.permission.WAKE_LOCK" />
            Log.w(TAG, "Couldn't display notification", e);
        }
    }

    private void displayStopHelp() {
        Intent intent = new Intent(RecorderService.this, DialogActivity.class);
        String message = String.format(getString(R.string.help_stop_message), getString(R.string.app_name));
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
        intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.help_stop_title));
        intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.help_stop_ok));
        intent.putExtra(DialogActivity.RESTART_EXTRA, true);
        intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, STOP_HELP_DISPLAYED_ACTION);
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

        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(NOTIFICATION_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        builder.setContentIntent(pendingIntent);

        startForeground(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private CharSequence getStatusString() {
        switch (state) {
            case INITIALIZING:
                return getString(R.string.notification_status_initializing);
            case INSTALLING:
                return getString(R.string.notification_status_installing);
            case INSTALLING_AUDIO:
                return getString(R.string.notification_status_installing_audio);
            case READY:
                return getString(R.string.notification_status_ready);
            case STARTING:
                return getString(R.string.notification_status_starting);
            case RECORDING:
                return getString(R.string.notification_status_recording);
            case STOPPING:
                return getString(R.string.notification_status_stopping);
            case ERROR:
                return getString(R.string.notification_status_error);
            case UNINSTALLING_AUDIO:
                return getString(R.string.notification_status_uninstalling_audio);
        }
        return "";
    }

    @Override
    public void suRequired() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.su_required_title));

                Intent suIntent = Utils.findSuIntent(RecorderService.this);

                if (suIntent == null) {
                    intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.su_required_message));
                    intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.su_required_help));
                    Intent helpIntent = new Intent(Intent.ACTION_VIEW);
                    helpIntent.setData(Uri.parse(getString(R.string.su_required_help_link)));
                    helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, helpIntent);
                } else {
                    CharSequence suName = Utils.getAppName(RecorderService.this, suIntent);
                    if (suName == null) {
                        suName = getString(R.string.su_default_name);
                    }
                    String message = getString(R.string.su_denied_message, suName, getString(R.string.app_name));
                    intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
                    intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, suIntent);
                    intent.putExtra(DialogActivity.POSITIVE_EXTRA, suName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
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
        intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, ERROR_DIALOG_CLOSED_ACTION);
        intent.putExtra(DialogActivity.REPORT_BUG_EXTRA, report);
        intent.putExtra(DialogActivity.REPORT_BUG_ERROR_EXTRA, errorCode);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Log.w(TAG, "displayErrorMessage: " + message);
        if (mRatingController != null) {
            mRatingController.resetSuccessCount();
        }

        if (restart) {
            mWatermark.hide();
            mRecorderOverlay.hide();
            Settings.getInstance().restoreShowTouches();
            mTimeController.reset();
            mScreenOffReceiver.unregister();
        } else {
            stopSelf();
        }
    }

    @Override
    public void startupError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.startup_error_message),recordingInfo.exitValue);
                displayErrorMessage(message, getString(R.string.error_dialog_title), false, true, recordingInfo.exitValue);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, STARTUP_ERROR, ERROR_ +recordingInfo.exitValue, null);
    }

    @Override
    public void recordingError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.recording_error_message),recordingInfo.exitValue);
                displayErrorMessage(message, getString(R.string.error_dialog_title), true, true,recordingInfo.exitValue);
                if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                    scanOutputAndNotify(R.string.recording_saved_toast);
                }
                logStats(recordingInfo);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + recordingInfo.exitValue, null);
    }

    @Override
    public void mediaRecorderError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.media_recorder_error_message),recordingInfo.exitValue);
                displayErrorMessage(message, getString(R.string.media_recorder_error_title), true, true,recordingInfo.exitValue);
                if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                    scanOutputAndNotify(R.string.recording_saved_toast);
                }
                logStats(recordingInfo);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + recordingInfo.exitValue, null);
    }

    @Override
    public void maxFileSizeReached(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                scanOutputAndNotify(R.string.max_file_size_reached_toast);
                reportRecordingStats(recordingInfo);
                reinitializeView();
                mRatingController.increaseSuccessCount();
                reinitialize();
            }
        });
    }

    @Override
    public void outputFileError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = String.format(getString(R.string.output_file_error_message), outputFile);
                displayErrorMessage(message, getString(R.string.output_file_error_title), true, false, recordingInfo.exitValue);
                logStats(recordingInfo);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ +recordingInfo.exitValue, null);
    }

    @Override
    public void microphoneBusyError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.microphone_busy_error_message));
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.microphone_busy_error_title));
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.microphone_busy_error_continue_mute));
                intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.settings_cancel));
                intent.putExtra(DialogActivity.RESTART_EXTRA, true);
                intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, RESTART_MUTE_ACTION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                logStats(recordingInfo);
                reinitialize();
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + recordingInfo.exitValue, null);
    }

    @Override
    public void openGlError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                displayErrorMessage(getString(R.string.opengl_error_message), getString(R.string.opengl_error_title), true, true,recordingInfo.exitValue);
                logStats(recordingInfo);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ +recordingInfo.exitValue, null);
    }

    @Override
    public void secureSurfaceError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                displayErrorMessage(getString(R.string.screen_protected_error_message), getString(R.string.screen_protected_error_title), true, false, recordingInfo.exitValue);
                logStats(recordingInfo);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ +recordingInfo.exitValue, null);
    }

    @Override
    public void audioConfigError(final RecordingInfo recordingInfo) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                displayErrorMessage(getString(R.string.audio_config_error_message), getString(R.string.audio_config_error_title), true, false,recordingInfo.exitValue);
                logStats(recordingInfo);
            }
        });
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + recordingInfo.exitValue, null);
    }

    @Override
    public void showSettings() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mRecorderOverlay.hide();
                settingsDisplayed = true;

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
                intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, TIMEOUT_DIALOG_CLOSED_ACTION);
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
        Intent intent = getPlayStoreIntent("timeout");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            EasyTracker.getTracker().sendEvent(ERROR, BUY_ERROR, TIMEOUT_DIALOG, null);
            displayErrorMessage(getString(R.string.buy_error_message), getString(R.string.buy_error_title), true, true, -1);
        }
        EasyTracker.getTracker().sendEvent(ACTION, BUY, TIMEOUT_DIALOG, null);
        stopSelf();
    }

    private Intent getPlayStoreIntent(String campaignName) {
        String uri = "market://details?id=com.iwobanas.screenrecorder.pro&referrer=utm_source%3Ddialog%26utm_campaign%3D" + campaignName;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        String action = intent.getAction();
        if (STOP_HELP_DISPLAYED_ACTION.equals(action)) {
            startRecording();
        } else if (TIMEOUT_DIALOG_CLOSED_ACTION.equals(action)) {
            isTimeoutDisplayed = false;
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                buyPro();
            } else {
                mRecorderOverlay.show();
            }
        } else if (RESTART_MUTE_ACTION.equals(action)) {
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                Settings.getInstance().setTemporaryMute(true);
                startRecordingWhenReady();
            } else {
                reinitializeView();
            }
        } else if (PLAY_ACTION.equals(action)) {
            playVideo(intent.getData());
        } else if (START_RECORDING_ACTION.equals(action)) {
            startRecordingWhenReady();
        } else if (ERROR_DIALOG_CLOSED_ACTION.equals(action)) {
            if (state != RecorderServiceState.READY) {
                reinitialize();
            }
            if (!settingsDisplayed) {
                mRecorderOverlay.show();
            }
        } else if (LICENSE_DIALOG_CLOSED.equals(action)) {
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) { // positive
                if (retryLicenseCheck) {
                    checkLicense();
                } else {
                    stopSelf();
                }
            } else { // negative
                retryLicenseCheck = false;
                if (state != RecorderServiceState.RECORDING && state != RecorderServiceState.STARTING) {
                    mRecorderOverlay.show();
                }
            }
        } else if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_ICON, null);
        } else {
            if (SETTINGS_CLOSED_ACTION.equals(action)) {
                settingsDisplayed = false;
            }
            if (SETTINGS_OPENED_ACTION.equals(action)) {
                settingsDisplayed = true;
                if (mRecorderOverlay.isVisible()) {
                    mRecorderOverlay.hide();
                }
            } else if (displayShutDownError) {
                shutDownError();
            } else if (mRatingController.shouldShow()) {
                mRecorderOverlay.hide();
                mRatingController.show();
            } else if (mRecorderOverlay.isVisible() && !firstCommand) {
                if (Utils.isMiUi(this)) {
                    showMiUiPopupError();
                }
                mRecorderOverlay.highlightPosition();
            } else {
                if (LOUNCHER_ACTION.equals(action) || NOTIFICATION_ACTION.equals(action)) {
                    mRecorderOverlay.animateShow();
                } else {
                    mRecorderOverlay.show();
                }
            }
            mCameraOverlay.applySettings();
        }
        firstCommand = false;
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_DESTROY, null);
        }
        if (mChecker != null) {
            mChecker.onDestroy();
        }
        startOnReady = false;
        mWatermark.hide();
        mWatermark.onDestroy();
        mCameraOverlay.hide();
        mCameraOverlay.onDestroy();
        mRecorderOverlay.animateHide();
        mRecorderOverlay.onDestroy();
        mNativeProcessRunner.destroy();
        mScreenOffReceiver.unregister();
        savePreferences();
        Settings.getInstance().restoreShowTouches();
        if (audioDriver.shouldUninstall()) {
            audioDriver.uninstall();
        }
        audioDriver.removeInstallListener(this);
        destroyed = true;
        markShutDownCorrectly();
    }

    private void checkShutDownCorrectly() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        if (!preferences.getBoolean(SHUT_DOWN_CORRECTLY, true) && audioDriver.shouldInstall()) {
            Settings.getInstance().setAudioSource(AudioSource.MUTE);
            displayShutDownError = true;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHUT_DOWN_CORRECTLY, false);
        editor.commit();
    }

    private void markShutDownCorrectly() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHUT_DOWN_CORRECTLY, true);
        editor.commit();
    }

    private void shutDownError() {
        displayShutDownError = false;
        String message = getString(R.string.internal_audio_disabled_message, getString(R.string.app_name));
        displayErrorMessage(message, getString(R.string.internal_audio_disabled_title), true, false, 2002);
    }

    private void showMiUiPopupError() {
        Log.w(TAG, "showMiUiPopupError");
        Intent intent = new Intent(RecorderService.this, DialogActivity.class);
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.miui_error_message, getString(R.string.app_name)));
        intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.miui_error_title));
        intent.putExtra(DialogActivity.RESTART_EXTRA, false);
        intent.putExtra(DialogActivity.REPORT_BUG_EXTRA, false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
        if (mChecker == null) {
            String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            mChecker = new LicenseChecker(
                    this, new ServerManagedPolicy(this,
                    new AESObfuscator(LICENSE_SALT, getPackageName(), deviceId)),
                    LICENSE_KEY
            );
        }

        mChecker.checkAccess(this);
    }

    @Override
    public void allow(int policyReason) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ALLOW_ + policyReason, null);

        if (policyReason != Policy.LICENSED) {
            Log.w(TAG, "err1: " + policyReason);
        }

        if (retryLicenseCheck) {
            retryLicenseCheck = false;
            mTaniosc = false;
            if (state != RecorderServiceState.RECORDING && state != RecorderServiceState.STARTING) {
                mRecorderOverlay.show();
            }
        }
    }

    @Override
    public void dontAllow(int policyReason) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_DONT_ALLOW_ + policyReason, null);
        Log.w(TAG, "err2: " + policyReason);

        if (policyReason == Policy.RETRY) { // 2.4%
            displayLicenseRetryDialog();
            retryLicenseCheck = true;
        } else if (policyReason == Policy.NOT_LICENSED) { // 7%
            displayNotLicensedDialog(R.string.license_not_licensed_message, R.string.free_timeout_buy, "not_licensed");
        } else if (policyReason == Policy.LICENSED) { // 0.06%
            displayNotLicensedDialog(R.string.license_error_message, R.string.license_play_store, "licensed");
            try {
                throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Unexpected response", e);
            }

        }
        mTaniosc = true;
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            mWatermark.show();
            mTimeController.start();
        }
    }

    @Override
    public void applicationError(int errorCode) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ERROR_ + errorCode, null);
        Log.w(TAG, "err3: " + errorCode);
        displayNotLicensedDialog(R.string.license_error_message, R.string.license_play_store, "license_error");

        mTaniosc = true;
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            mWatermark.show();
            mTimeController.start();
        }
    }


    private void displayLicenseRetryDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.license_retry_message));
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.license_title));
                intent.putExtra(DialogActivity.RESTART_EXTRA, true);
                intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, LICENSE_DIALOG_CLOSED);
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.license_retry));
                intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.license_continue_as_free));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                mRecorderOverlay.hide();
            }
        });
    }

    private void displayNotLicensedDialog(final int messageResource, final int buyButtonResource, final String campaignName) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(RecorderService.this, DialogActivity.class);
                intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(messageResource));
                intent.putExtra(DialogActivity.TITLE_EXTRA, getString(R.string.license_title));
                intent.putExtra(DialogActivity.RESTART_EXTRA, true);
                intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, LICENSE_DIALOG_CLOSED);
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(buyButtonResource));
                intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, getPlayStoreIntent(campaignName));
                intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.license_continue_as_free));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                mRecorderOverlay.hide();
            }
        });
    }

    @Override
    public void onInstall(InstallationStatus status) {
        if (closing && (status == InstallationStatus.NOT_INSTALLED || status == InstallationStatus.UNSPECIFIED)) {
            stopSelf();
        } else if (status == InstallationStatus.INSTALLATION_FAILURE) {
            audioDriverInstallationFailure();
        } else if (status == InstallationStatus.UNSTABLE) {
            audioDriverUnstable();
        } else if (status == InstallationStatus.INSTALLING) {
            setState(RecorderServiceState.INSTALLING_AUDIO);
        } else if (status == InstallationStatus.UNINSTALLING) {
            setState(RecorderServiceState.UNINSTALLING_AUDIO);
        } else {
            checkReady();
        }
    }

    private void audioDriverInstallationFailure() {
        Settings.getInstance().setAudioSource(AudioSource.MUTE);
        String message = getString(R.string.internal_audio_installation_error_message, getString(R.string.settings_audio_mute));
        displayErrorMessage(message, getString(R.string.internal_audio_installation_error_title), true, true, 2000);
    }

    private void audioDriverUnstable() {
        Settings.getInstance().setAudioSource(AudioSource.MUTE);
        String message = getString(R.string.internal_audio_unstable_message, getString(R.string.settings_audio_mute));
        displayErrorMessage(message, getString(R.string.internal_audio_unstable_title), true, true, 2001);
    }

    private static enum RecorderServiceState {
        INSTALLING,
        INITIALIZING,
        INSTALLING_AUDIO,
        READY,
        STARTING,
        RECORDING,
        STOPPING,
        ERROR,
        UNINSTALLING_AUDIO
    }
}
