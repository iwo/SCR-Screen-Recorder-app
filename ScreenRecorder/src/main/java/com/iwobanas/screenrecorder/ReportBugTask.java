package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.iwobanas.screenrecorder.Tracker.*;

public class ReportBugTask extends AsyncTask<Void, Void, Integer> {
    private static final String TAG = "scr_ReportBugTask";

    public ReportBugTask(Context context, int errorCode) {
        this.context = context.getApplicationContext();
        this.errorCode = errorCode;
    }

    private Context context;

    private int errorCode;

    private File reportFile;

    @Override
    protected Integer doInBackground(Void... params) {
        reportFile = new File(context.getExternalCacheDir(), "logcat.txt");
        try {
            FileOutputStream outputStream = new FileOutputStream(reportFile);
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Can't create report file at: " + reportFile.getAbsolutePath(), e);
            reportFile = new File("/sdcard", "logcat.txt");
            try {
                FileOutputStream outputStream = new FileOutputStream(reportFile);
                outputStream.close();
            } catch (IOException ee) {
                Log.e(TAG, "Can't create report file at: " + reportFile.getAbsolutePath(), ee);
                return -1;
            }
        }

        int exitValue = NativeCommands.getInstance().logcat(reportFile.getAbsolutePath());

        if (exitValue != 0) {
            Log.e(TAG, "Error running logcat command: " + exitValue);
            try {
                reportFile = new File("/sdcard/", "scr_logcat" + System.currentTimeMillis() + ".txt");
                String out = reportFile.getAbsolutePath();
                String command = "/system/bin/logcat -d -v threadtime -f " + out + "*:V";

                exitValue = runAndWait(command);

                if (exitValue != 0) { // retry
                    Thread.sleep(2000);
                    exitValue = runAndWait(command);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error running logcat", e);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error running logcat", e);
            }
        }
        return exitValue;
    }

    private int runAndWait(String command) throws InterruptedException, IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
        process.waitFor();
        return process.exitValue();
    }

    @Override
    protected void onPostExecute(Integer exitValue) {
        if (exitValue != 0) {
            return;
        }
        EasyTracker.getInstance().setContext(context); // for some reason context sometimes wasn't set here
        EasyTracker.getTracker().sendEvent(ACTION, BUG, REPORT, null);
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"scr.screen.recorder@gmail.com"});
        String version = Utils.getAppVersionName(context);
        String subject = context.getString(R.string.error_report_subject) + " " + context.getString(R.string.app_name) + " " + version + " - " + errorCode + " - " + Build.DEVICE + " - " + Build.VERSION.RELEASE ;
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.error_report_text));
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(reportFile));
        Intent chooserIntent = Intent.createChooser(emailIntent, context.getString(R.string.error_report_chooser));
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }
}
