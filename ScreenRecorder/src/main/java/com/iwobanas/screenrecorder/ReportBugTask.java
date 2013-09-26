package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.google.analytics.tracking.android.EasyTracker;

import java.io.File;
import java.io.IOException;

import static com.iwobanas.screenrecorder.Tracker.*;

public class ReportBugTask extends AsyncTask<Void, Void, Integer> {

    public ReportBugTask(Context context, int errorCode) {
        this.context = context;
        this.errorCode = errorCode;
    }

    private Context context;

    private int errorCode;

    private File output;

    @Override
    protected Integer doInBackground(Void... params) {
        int exitValue = -1;
        try {
            output = new File("/sdcard/", "scr_logcat" + System.currentTimeMillis() + ".txt");
            String out = output.getAbsolutePath();
            String command = "/system/bin/logcat -d -v threadtime -f " + out;

            exitValue = runAndWait(command);

            if (exitValue != 0) { // retry
                Thread.sleep(2000);
                exitValue = runAndWait(command);
            }

        } catch (IOException e) {
            EasyTracker.getTracker().sendException("ReportBugTask", e, false);
        } catch (InterruptedException e) {
            EasyTracker.getTracker().sendException("ReportBugTask", e, false);
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
        EasyTracker.getTracker().sendEvent(ACTION, BUG, REPORT, null);
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"scr.screen.recorder@gmail.com"});
        String subject = context.getString(R.string.error_report_subject) + " " + context.getString(R.string.app_name) + " - " + errorCode + " - " + Build.DEVICE + " - " + Build.VERSION.RELEASE ;
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.error_report_text));
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(output));
        Intent chooserIntent = Intent.createChooser(emailIntent, context.getString(R.string.error_report_chooser));
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }
}
