package com.iwobanas.screenrecorder;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import java.io.File;
import java.io.IOException;

import static com.iwobanas.screenrecorder.Tracker.ERROR;
import static com.iwobanas.screenrecorder.Tracker.INSTALLATION_ERROR;

public class InstallExecutableAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "scr_InstallExecutableAsyncTask";
    private IRecorderService service;
    private Context context;
    private File executable;

    public InstallExecutableAsyncTask(IRecorderService service, Context context) {
        this.service = service;
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        executable = new File(context.getFilesDir(), "screenrec");
        try {
            if (Utils.isArm()) {
                Utils.extractResource(context, R.raw.screenrec, executable);
            } else if (Utils.isX86()) {
                Utils.extractResource(context, R.raw.screenrec_x86, executable);
            } else {
                service.cpuNotSupportedError();
                return false;
            }

            if (!executable.setExecutable(true, false)) {
                Log.w(TAG, "Can't set executable property on " + executable.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "Can't install native executable", e);
            service.installationError();
            EasyTracker.getTracker().sendEvent(ERROR, INSTALLATION_ERROR, INSTALLATION_ERROR, null);
            EasyTracker.getTracker().sendException(Thread.currentThread().getName(), e, false);
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (result) {
            service.executableInstalled(executable.getAbsolutePath());
        }
    }
}
