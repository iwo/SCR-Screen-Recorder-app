package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.google.analytics.tracking.android.EasyTracker;

import java.io.File;
import java.io.IOException;

import static com.iwobanas.screenrecorder.Tracker.*;

public class ReportBugTask extends AsyncTask<Void, Void, Void> {

    public ReportBugTask(Context context) {
        this.context = context;
    }

    private Context context;

    private File output;

    @Override
    protected Void doInBackground(Void... params) {
        try {
            output = new File(context.getFilesDir(), "logcat" + System.currentTimeMillis() + ".txt");
            String out = output.getAbsolutePath();

            Process process = Runtime.getRuntime()
                    .exec(new String[]{"su", "-c", "logcat -d -v threadtime -f " + out
                            + "; chmod 666 " + out});

            process.waitFor();

        } catch (IOException e) {
            EasyTracker.getTracker().sendException("ReportBugTask", e, false);
        } catch (InterruptedException e) {
            EasyTracker.getTracker().sendException("ReportBugTask", e, false);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void param) {
        EasyTracker.getTracker().sendEvent(ACTION, BUG, REPORT, null);
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"scr.screen.recorder@gmail.com"});
        String subject = context.getString(R.string.error_report_subject) + context.getString(R.string.app_name);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.error_report_text));
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(output));
        Intent chooserIntent = Intent.createChooser(emailIntent, context.getString(R.string.error_report_chooser));
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooserIntent);
    }
}
