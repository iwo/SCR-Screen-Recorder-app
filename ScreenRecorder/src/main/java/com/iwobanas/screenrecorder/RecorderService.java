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
import static com.iwobanas.screenrecorder.Tracker.LICENSE;
import static com.iwobanas.screenrecorder.Tracker.LICENSE_ALLOW_;
import static com.iwobanas.screenrecorder.Tracker.LICENSE_DONT_ALLOW_;
import static com.iwobanas.screenrecorder.Tracker.LICENSE_ERROR_;
import static com.iwobanas.screenrecorder.Tracker.RECORDING;
import static com.iwobanas.screenrecorder.Tracker.SETTINGS;
import static com.iwobanas.screenrecorder.Tracker.SIZE;
import static com.iwobanas.screenrecorder.Tracker.START;
import static com.iwobanas.screenrecorder.Tracker.STATS;
import static com.iwobanas.screenrecorder.Tracker.STOP;
import static com.iwobanas.screenrecorder.Tracker.STOP_DESTROY;
import static com.iwobanas.screenrecorder.Tracker.STOP_ICON;
import static com.iwobanas.screenrecorder.Tracker.TIME;
import static com.iwobanas.screenrecorder.Tracker.TIMEOUT_DIALOG;

public class RecorderService extends Service implements IRecorderService, LicenseCheckerCallback, AudioDriver.OnInstallListener, IRecordingProcess.RecordingProcessObserver {

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
    public static final String ENABLE_ROOT_ACTION = "scr.intent.action.ENABLE_ROOT_ACTION";

    public static final String SET_PROJECTION_ACTION = "scr.intent.action.SET_PROJECTION";
    public static final String PROJECTION_DATA_EXTRA = "projection_data";
    public static final String PROJECTION_DENY_ACTION = "scr.intent.action.PROJECTION_DENY";

    private static final String TAG = "scr_RecorderService";
    private static final String STOP_HELP_DISPLAYED_PREFERENCE = "stopHelpDisplayed";
    private static final String SHUT_DOWN_CORRECTLY = "SHUT_DOWN_CORRECTLY";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    // Licensing
    private static final byte[] LICENSE_SALT = new byte[]{95, -9, 7, -80, -79, -72, 3, -116, 95, 79, -18, 63, -124, -85, -71, -2, -73, -37, 47, 122};

    private IScreenOverlay watermarkOverlay = new WatermarkOverlay(this);
    private RecorderOverlay recorderOverlay = new RecorderOverlay(this, this);
    private CameraOverlay cameraOverlay;
    private ScreenOffReceiver screenOffReceiver = new ScreenOffReceiver(this, this);
    private IRecordingProcess nativeProcessRunner;
    private IRecordingProcess projectionThreadRunner;
    private RecordingTimeController recordingTimeController = new RecordingTimeController(this);
    private RatingController ratingController;
    private AudioDriver audioDriver;
    private ErrorDialogHelper errorDialogHelper;
    private Handler handler;
    private File outputFile;
    private RecorderServiceState state = RecorderServiceState.INITIALIZING;
    private boolean isTimeoutDisplayed;
    private boolean startOnReady;
    private boolean projectionDenied;
    private long recordingStartTime;
    private boolean free = true;
    private boolean retryLicenseCheck = false;
    private boolean firstCommand = true;
    private boolean closing = false;
    private boolean destroyed = false;
    private boolean settingsDisplayed = false;
    private boolean displayShutDownError = false;
    private Toast cantStartToast;

    // Preferences
    private boolean stopHelpDisplayed;
    private LicenseChecker licenseChecker;

    @Override
    public void onCreate() {
        EasyTracker.getInstance().setContext(getApplicationContext());
        initializeExceptionParser();
        handler = new Handler();

        errorDialogHelper = new ErrorDialogHelper(this);
        Settings.initialize(this);
        Settings s = Settings.getInstance();

        //TODO: allow switching to non-root mode from this dialog as well
        if (s.isRootFlavor() && (Build.VERSION.SDK_INT < 15 || Build.VERSION.SDK_INT == 20 || Build.VERSION.SDK_INT > 21)) {
            displayErrorMessage(getString(R.string.android_version_error_message), getString(R.string.android_version_error_title), false, false, -1);
        }

        free = BuildConfig.FLAVOR_price.startsWith("f"); // free

        audioDriver = Settings.getInstance().getAudioDriver();
        audioDriver.addInstallListener(this);
        checkShutDownCorrectly();

        cameraOverlay = new CameraOverlay(this);
        cameraOverlay.applySettings();

        ratingController = new RatingController(this);

        readPreferences();

        nativeProcessRunner = new NativeProcessRunner(this);
        nativeProcessRunner.addObserver(this);
        projectionThreadRunner = new ProjectionThreadRunner(this);
        projectionThreadRunner.addObserver(this);

        recorderOverlay.animateShow();
        reinitialize();

        if (!free) {
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

    private void setState(RecorderServiceState state) {
        if (this.state == state)
            return;
        Log.v(TAG, state.name());
        this.state = state;
        if (!destroyed) {
            startForeground();
        }
    }

    private boolean useProjection() {
        return Settings.getInstance().isNoRootVideoEncoder();
    }

    @Override
    public void startRecording() {
        if (cantStartToast != null) {
            cantStartToast.cancel();
        }

        if (state == RecorderServiceState.INITIALIZING && useProjection()) {
            projectionDenied = false;
            projectionThreadRunner.initialize(); // request initialization again
            startRecordingWhenReady();
            return;
        }

        if (state != RecorderServiceState.READY) {
            cantStartToast = Toast.makeText(this, getString(R.string.can_not_start_toast, getStatusString()), Toast.LENGTH_SHORT);
            cantStartToast.show();
            return;
        }
        if (!stopHelpDisplayed) {
            recorderOverlay.hide();
            displayStopHelp();
            return;
        }
        Log.i(TAG, "start deviceId: " + getDeviceId());
        setState(RecorderServiceState.STARTING);
        recorderOverlay.hide();
        cameraOverlay.setTouchable(false);
        if (free) {
            watermarkOverlay.show();
            recordingTimeController.start();
        }
        Settings.getInstance().applyShowTouches();
        if (Settings.getInstance().getStopOnScreenOff()) {
            screenOffReceiver.register();
        }
        audioDriver.startRecording();
        outputFile = getOutputFile();
        if (useProjection()) {
            projectionThreadRunner.start(outputFile.getAbsolutePath(), getRotation());
        } else {
            nativeProcessRunner.start(outputFile.getAbsolutePath(), getRotation());
        }
        recordingStartTime = System.currentTimeMillis();

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
        if (useProjection()) {
            projectionThreadRunner.stop();
        } else {
            nativeProcessRunner.stop();
        }
        recordingTimeController.reset();
        cameraOverlay.setTouchable(true);
    }

    private void playVideo(Uri uri) {
        recorderOverlay.hide();
        cameraOverlay.hide();

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
        recorderOverlay.animateHide();
        cameraOverlay.hide();
        if (audioDriver.shouldUninstall()) {
            audioDriver.uninstall();
        } else {
            stopSelf();
        }
    }

    @Override
    public void onStateChange(IRecordingProcess process, RecordingProcessState state, RecordingInfo recordingInfo) {

        switch (state) {
            case READY:
                nextInitializationStep();
                break;
            case STARTING:
                setState(RecorderServiceState.STARTING);
                break;
            case RECORDING:
                setState(RecorderServiceState.RECORDING);
                break;
            case STOPPING:
                setState(RecorderServiceState.STOPPING);
                break;
            case FINISHED:
                recordingFinished(recordingInfo);
                break;
            case MAX_FILE_SIZE_REACHED:
                maxFileSizeReached(recordingInfo);
                break;
            case MICROPHONE_BUSY_ERROR:
                reinitialize(); // reinitialize so that we're ready to start another recording
                //TODO: make sure that recording saved notification is not displayed in this case
                break;
        }

        if (state.isError()) {
            errorDialogHelper.onStateChange(process, state, recordingInfo);
            if (recordingInfo != null) {
                logStats(recordingInfo);
            }
            if (ratingController != null) {
                ratingController.resetSuccessCount();
            }
            if (state.isCritical()) {
                stopSelf();
            } else {
                closeForError();
            }

            if (outputFile != null && outputFile.exists() && outputFile.length() > 0) {
                scanOutputAndNotify(R.string.recording_saved_toast);
            }
        }
    }

    private void nextInitializationStep() {
        boolean root = Settings.getInstance().isRootEnabled();
        if (root && !nativeProcessRunner.isReady()) {
            nativeProcessRunner.initialize();
        } else if (root && audioDriver.getInstallationStatus() == InstallationStatus.NEW) {
            audioDriver.check();
        } else if (root && audioDriver.shouldInstall()) {
            audioDriver.install();
        } else if (useProjection() && projectionDenied) {
            Log.w(TAG, "Not progressing with initialization because last projection request was cancelled");
        } else if (useProjection() && !projectionThreadRunner.isReady()) {
            projectionThreadRunner.initialize();
        } else {
            setState(RecorderServiceState.READY);
            if (startOnReady) {
                startOnReady = false;
                startRecording();
            }
        }
    }

    @Override
    public void onInstall(InstallationStatus status) {
        switch (status) {
            case NEW:
            case CHECKING:
                break;

            case INSTALLING:
                setState(RecorderServiceState.INSTALLING_AUDIO);
                break;

            case INSTALLATION_FAILURE:
                audioDriverInstallationFailure();
                break;

            case UNSTABLE:
                audioDriverUnstable();
                break;

            case UNINSTALLING:
                setState(RecorderServiceState.UNINSTALLING_AUDIO);
                break;

            case NOT_INSTALLED:
            case UNSPECIFIED:
                if (closing) {
                    stopSelf();
                    return;
                } // else fall through
            case INSTALLED:
            case OUTDATED:
                nextInitializationStep();
        }
    }

    private void recordingFinished(final RecordingInfo recordingInfo) {
        scanOutputAndNotify(R.string.recording_saved_toast);
        reportRecordingStats(recordingInfo);
        reinitializeView();
        reinitialize();
        ratingController.increaseSuccessCount();
    }

    private void reinitializeView() {
        if (free) {
            watermarkOverlay.hide();
        }
        Settings.getInstance().restoreShowTouches();
        if (!isTimeoutDisplayed) {
            recorderOverlay.animateShow();
        }
        cameraOverlay.setTouchable(true);
    }

    private void reinitialize() {
        recordingTimeController.reset();
        screenOffReceiver.unregister();

        setState(RecorderServiceState.INITIALIZING);

        nextInitializationStep();
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
        notificationSaved(scanFile(outputFile));
    }

    private void logStats(RecordingInfo recordingInfo) {
        if (outputFile != null) { // I can't figure out when it's null but analytics shows NPE here
            recordingInfo.size = (int) (outputFile.length() / 1024l);
        }
        recordingInfo.time = (int) ((System.currentTimeMillis() - recordingStartTime) / 1000l);
        new RecordingStatsAsyncTask(this, recordingInfo).execute();
        if (Settings.getInstance().getAudioSource() == AudioSource.INTERNAL) {
            audioDriver.logStats(recordingInfo);
        }
    }

    private Uri scanFile(File file) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.TITLE, file.getName());
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, recordingStartTime);
        contentValues.put(MediaStore.Video.Media.DATE_ADDED, recordingStartTime / 1000);
        contentValues.put(MediaStore.Video.Media.DATE_MODIFIED, recordingStartTime / 1000);

        try {
            return getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting video content values", e);
            return Uri.fromFile(file);
        }
    }

    private void notificationSaved(Uri uri) {
        String message = String.format(getString(R.string.recording_saved_message), outputFile.getName());
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_notification_saved)
                        .setContentTitle(getString(R.string.recording_saved_title))
                        .setContentText(message);

        Intent playIntent = new Intent(this, RecorderService.class);
        playIntent.setAction(PLAY_ACTION);
        playIntent.setData(uri);
        mBuilder.setContentIntent(PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_CANCEL_CURRENT));
        mBuilder.addAction(R.drawable.ic_menu_share, getString(R.string.notification_action_share), getSharePendingIntent(uri));

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

    private PendingIntent getSharePendingIntent(Uri uri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        Intent chooserIntent = Intent.createChooser(shareIntent, null);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, chooserIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        stopHelpDisplayed = true;
    }

    private void startForeground() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.app_full_name));
        if (state == RecorderServiceState.RECORDING) {
            builder.setUsesChronometer(true);
            builder.setWhen(recordingStartTime);
        } else {
            builder.setWhen(0);
        }
        builder.setContentText(getStatusString());

        if (!free && Settings.getInstance().getHideIcon()) {
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
                return useProjection() ? getString(R.string.notification_status_initializing_no_root) : getString(R.string.notification_status_initializing);
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


    private void displayErrorMessage(final String message, final String title, final boolean restart, boolean report, int errorCode) {
        errorDialogHelper.showError(message, title, restart, report, errorCode);
        if (ratingController != null) {
            ratingController.resetSuccessCount();
        }
        if (restart) {
            closeForError();
        } else {
            stopSelf();
        }
    }

    private void closeForError() {
        watermarkOverlay.hide();
        recorderOverlay.hide();
        Settings.getInstance().restoreShowTouches();
        recordingTimeController.reset();
        screenOffReceiver.unregister();
    }

    private void maxFileSizeReached(final RecordingInfo recordingInfo) {
        scanOutputAndNotify(R.string.max_file_size_reached_toast);
        reportRecordingStats(recordingInfo);
        reinitializeView();
        ratingController.increaseSuccessCount();
        reinitialize();
    }

    @Override
    public void showSettings() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                recorderOverlay.hide();
                settingsDisplayed = true;

                Intent intent = new Intent(RecorderService.this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                ratingController.resetSuccessCount();
            }
        });
    }

    @Override
    public void showTimeoutDialog() {
        handler.post(new Runnable() {
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

    private String getDeviceId() {
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
        //TODO:update with new package Id
        String uri;
        if (Settings.getInstance().isRootFlavor()) {
            uri = "market://details?id=com.iwobanas.screenrecorder.pro&referrer=utm_source%3Ddialog%26utm_campaign%3D" + campaignName;
        } else {
            uri = "market://details?id=com.iwobanas.screenrecorder.noroot.pro&referrer=utm_source%3Ddialog%26utm_campaign%3D" + campaignName;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }


    private void denyProjectionError() {
        projectionDenied = true;
        displayErrorMessage(
                getString(R.string.projection_deny_error_message),
                getString(R.string.projection_deny_error_title),
                true, false, 510);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground();
        String action = intent.getAction();
        if (STOP_HELP_DISPLAYED_ACTION.equals(action)) {
            startRecording();
        } else if (SET_PROJECTION_ACTION.equals(action)) {
            Intent data = intent.getParcelableExtra(PROJECTION_DATA_EXTRA);
            //TODO: decide if it's more elegant to set this data through service or through static field
            ((ProjectionThreadRunner) projectionThreadRunner).setProjectionData(data);
        } else if (PROJECTION_DENY_ACTION.equals(action)) {
            denyProjectionError();
        } else if (TIMEOUT_DIALOG_CLOSED_ACTION.equals(action)) {
            isTimeoutDisplayed = false;
            if (intent.getBooleanExtra(DialogActivity.POSITIVE_EXTRA, false)) {
                buyPro();
            } else {
                recorderOverlay.show();
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
                recorderOverlay.show();
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
                    recorderOverlay.show();
                }
            }
        } else if (ENABLE_ROOT_ACTION.equals(action)) {
            reinitialize();
        } else if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            stopRecording();
            EasyTracker.getTracker().sendEvent(ACTION, STOP, STOP_ICON, null);
        } else {
            if (SETTINGS_CLOSED_ACTION.equals(action)) {
                settingsDisplayed = false;
            }
            if (SETTINGS_OPENED_ACTION.equals(action)) {
                settingsDisplayed = true;
                if (recorderOverlay.isVisible()) {
                    recorderOverlay.hide();
                }
            } else if (displayShutDownError) {
                shutDownError();
            } else if (ratingController.shouldShow()) {
                recorderOverlay.hide();
                ratingController.show();
            } else if (recorderOverlay.isVisible() && !firstCommand) {
                if (Utils.isMiUi(this)) {
                    showMiUiPopupError();
                }
                recorderOverlay.highlightPosition();
            } else {
                if (LOUNCHER_ACTION.equals(action) || NOTIFICATION_ACTION.equals(action)) {
                    recorderOverlay.animateShow();
                } else {
                    recorderOverlay.show();
                }
            }
            cameraOverlay.applySettings();
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
        if (licenseChecker != null) {
            licenseChecker.onDestroy();
        }
        startOnReady = false;
        watermarkOverlay.hide();
        watermarkOverlay.onDestroy();
        cameraOverlay.hide();
        cameraOverlay.onDestroy();
        recorderOverlay.animateHide();
        recorderOverlay.onDestroy();
        nativeProcessRunner.destroy();
        projectionThreadRunner.destroy();
        screenOffReceiver.unregister();
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
        stopHelpDisplayed = preferences.getBoolean(STOP_HELP_DISPLAYED_PREFERENCE, false);
    }

    private void savePreferences() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(STOP_HELP_DISPLAYED_PREFERENCE, stopHelpDisplayed);
        editor.commit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void checkLicense() {
        if (licenseChecker == null) {
            String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            licenseChecker = new LicenseChecker(
                    this, new ServerManagedPolicy(this,
                    new AESObfuscator(LICENSE_SALT, getPackageName(), deviceId)),
                    LicenseInfo.getLicenseKey()
            );
        }

        licenseChecker.checkAccess(this);
    }

    @Override
    public void allow(int policyReason) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ALLOW_ + policyReason, null);

        if (policyReason != Policy.LICENSED) {
            Log.w(TAG, "err1: " + policyReason);
        }

        if (retryLicenseCheck) {
            retryLicenseCheck = false;
            free = false;
            if (state != RecorderServiceState.RECORDING && state != RecorderServiceState.STARTING) {
                recorderOverlay.show();
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
        free = true;
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            watermarkOverlay.show();
            recordingTimeController.start();
        }
    }

    @Override
    public void applicationError(int errorCode) {
        EasyTracker.getTracker().sendEvent(STATS, LICENSE, LICENSE_ERROR_ + errorCode, null);
        Log.w(TAG, "err3: " + errorCode);
        displayNotLicensedDialog(R.string.license_error_message, R.string.license_play_store, "license_error");

        free = true;
        if (state == RecorderServiceState.RECORDING || state == RecorderServiceState.STARTING) {
            watermarkOverlay.show();
            recordingTimeController.start();
        }
    }


    private void displayLicenseRetryDialog() {
        handler.post(new Runnable() {
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
                recorderOverlay.hide();
            }
        });
    }

    private void displayNotLicensedDialog(final int messageResource, final int buyButtonResource, final String campaignName) {
        handler.post(new Runnable() {
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
                recorderOverlay.hide();
            }
        });
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
