package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.ReportBugTask;

public class SettingsActivity extends Activity {
    public static final String TAG = "scr_SettingsActivity";
    private SettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            fragment = new SettingsFragment();

            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_restore_defaults:
                Settings.getInstance().restoreDefault();
                if (fragment != null) {
                    fragment.updateValues();
                }
                return true;
            case R.id.settings_send_bug_report:
                sendBugReport();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendBugReport() {
        new ReportBugTask(getApplicationContext(), 1000).execute();
    }

}


