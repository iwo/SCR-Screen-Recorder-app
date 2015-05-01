package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import static com.iwobanas.screenrecorder.Tracker.ERROR;
import static com.iwobanas.screenrecorder.Tracker.ERROR_;
import static com.iwobanas.screenrecorder.Tracker.RECORDING_ERROR;
import static com.iwobanas.screenrecorder.Tracker.STARTUP_ERROR;

public class ErrorDialogHelper implements IRecordingProcess.RecordingProcessObserver {

    private static final String TAG = "scr_ErrorDialogHelper";
    private Context context;

    public ErrorDialogHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onStateChange(IRecordingProcess process, RecordingProcessState state, RecordingInfo recordingInfo) {
        if (!state.isError())
            return;

        String title = getString(R.string.error_dialog_title);
        String message = getString(R.string.unknown_error_message, 600);
        boolean report = false;

        switch (state) {
            case SU_ERROR:
                showSuError();
                return;

            case MICROPHONE_BUSY_ERROR:
                showMicrophoneBusyError(recordingInfo);
                return;

            case CPU_NOT_SUPPORTED_ERROR:
                //noinspection deprecation
                message = getString(R.string.cpu_error_message, Build.CPU_ABI, getString(R.string.app_name));
                title = getString(R.string.cpu_error_title);
                report = false;
                break;

            case INSTALLATION_ERROR:
                message = getString(R.string.installation_error_message, getString(R.string.app_name));
                title = getString(R.string.installation_error_title);
                report = false;
                break;

            case UNKNOWN_STARTUP_ERROR:
                message = getString(R.string.unknown_error_message, recordingInfo.exitValue);
                title = getString(R.string.error_dialog_title);
                report = true;
                break;

            case VIDEO_CODEC_ERROR:
                message = getString(R.string.video_codec_error_message, recordingInfo.exitValue);
                title = getString(R.string.video_codec_error_title);
                report = true;
                break;

            case UNKNOWN_RECORDING_ERROR:
                message = getString(R.string.unknown_error_message, recordingInfo.exitValue);
                title = getString(R.string.error_dialog_title);
                report = true;
                break;

            case MEDIA_RECORDER_ERROR:
                message = getString(R.string.media_recorder_error_message, recordingInfo.exitValue);
                title = getString(R.string.media_recorder_error_title);
                report = true;
                break;
            case OUTPUT_FILE_ERROR:
                message = getString(R.string.output_file_error_message, recordingInfo.file);
                title = getString(R.string.output_file_error_title);
                report = true; // this used to be false but due to ext sd write issues it may be true now
                break;

            case OPEN_GL_ERROR:
                message = getString(R.string.opengl_error_message);
                title = getString(R.string.opengl_error_title);
                report = true;
                break;

            case SECURE_SURFACE_ERROR:
                message = getString(R.string.screen_protected_error_message);
                title = getString(R.string.screen_protected_error_title);
                report = false;
                break;

            case AUDIO_CONFIG_ERROR:
                message = getString(R.string.audio_config_error_message);
                title = getString(R.string.audio_config_error_title);
                report = false;
                break;

            case SELINUX_ERROR:
                message = getString(R.string.selinux_error_message);
                title = getString(R.string.selinux_error_title);
                report = false;
                break;
        }

        showError(message, title, !state.isCritical(), report, recordingInfo == null ? -1 : recordingInfo.exitValue);

        if (recordingInfo != null) {
            EasyTracker.getTracker().sendEvent(ERROR, state.isCritical() ? STARTUP_ERROR : RECORDING_ERROR, ERROR_ + recordingInfo.exitValue, null);
        }
    }

    private Intent createDialogIntent(String title) {
        Intent intent = new Intent(context, DialogActivity.class);
        intent.putExtra(DialogActivity.TITLE_EXTRA, title);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    protected void showError(final String message, final String title, boolean restart, boolean report, int errorCode) {
        Intent intent = createDialogIntent(title);
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
        intent.putExtra(DialogActivity.RESTART_EXTRA, restart);
        intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, RecorderService.ERROR_DIALOG_CLOSED_ACTION);
        intent.putExtra(DialogActivity.REPORT_BUG_EXTRA, report);
        intent.putExtra(DialogActivity.REPORT_BUG_ERROR_EXTRA, errorCode);

        context.startActivity(intent);
        Log.w(TAG, "showError: " + message);
    }

    private void showSuError() {
        Intent intent = createDialogIntent(getString(R.string.su_required_title));
        Intent suIntent = Utils.findSuIntent(context);
        CharSequence suName = null;
        if (suIntent != null) {
            suName = Utils.getAppName(context, suIntent);
            if (suName == null) {
                suName = getString(R.string.su_default_name);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String message = getString(R.string.su_required_lollipop_message, getString(R.string.app_name), getString(R.string.free_app_no_root_name));
            if (suIntent != null) {
                message += " " + getString(R.string.su_required_lollipop_denied_message, suName, getString(R.string.app_name));
                intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, suIntent);
                intent.putExtra(DialogActivity.POSITIVE_EXTRA, suName);
            }
            intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.su_required_no_root_mode));
            Intent noRootModeIntent = new Intent(context, NoRootModeActivity.class);
            noRootModeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            noRootModeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(DialogActivity.NEGATIVE_INTENT_EXTRA, noRootModeIntent);
            intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
        } else if (suIntent == null) {
            intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.su_required_message));
            intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.su_required_help));
            Intent helpIntent = new Intent(Intent.ACTION_VIEW);
            helpIntent.setData(Uri.parse(getString(R.string.su_required_help_link)));
            helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, helpIntent);
        } else {
            String message = getString(R.string.su_denied_message, suName, getString(R.string.app_name));
            intent.putExtra(DialogActivity.MESSAGE_EXTRA, message);
            intent.putExtra(DialogActivity.POSITIVE_INTENT_EXTRA, suIntent);
            intent.putExtra(DialogActivity.POSITIVE_EXTRA, suName);
        }
        context.startActivity(intent);
    }

    public void showMicrophoneBusyError(RecordingInfo recordingInfo) {
        Intent intent = createDialogIntent(getString(R.string.microphone_busy_error_title));
        intent.putExtra(DialogActivity.MESSAGE_EXTRA, getString(R.string.microphone_busy_error_message));
        intent.putExtra(DialogActivity.POSITIVE_EXTRA, getString(R.string.microphone_busy_error_continue_mute));
        intent.putExtra(DialogActivity.NEGATIVE_EXTRA, getString(R.string.settings_cancel));
        intent.putExtra(DialogActivity.RESTART_EXTRA, true);
        intent.putExtra(DialogActivity.RESTART_ACTION_EXTRA, RecorderService.RESTART_MUTE_ACTION);
        context.startActivity(intent);
        EasyTracker.getTracker().sendEvent(ERROR, RECORDING_ERROR, ERROR_ + recordingInfo.exitValue, null);
    }

    private String getString(int resId) {
        return context.getString(resId);
    }

    private String getString(int resId, Object... formatArgs) {
        return context.getString(resId, formatArgs);
    }
}
