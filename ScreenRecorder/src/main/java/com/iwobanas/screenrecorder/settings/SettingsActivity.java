package com.iwobanas.screenrecorder.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.view.Menu;
import android.view.MenuItem;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.RecorderService;
import com.iwobanas.screenrecorder.ReportBugTask;

public class SettingsActivity extends Activity {
    public static final String TAG = "scr_SettingsActivity";
    private SettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Settings.initialize(this);
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

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
        if (!Settings.getInstance().isRootFlavor()) {
            return false;
        }
        getMenuInflater().inflate(R.menu.settings, menu);
        menu.findItem(R.id.settings_show_advanced).setChecked(Settings.getInstance().getShowAdvanced());
        menu.findItem(R.id.settings_show_unstable).setChecked(Settings.getInstance().getShowUnstable());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_restore_defaults:
                Settings.getInstance().restoreDefault();
                if (fragment != null) {
                    fragment.updateEntries();
                    fragment.updateValues();
                }
                return true;
            case R.id.settings_send_bug_report:
                sendBugReport();
                return true;
            case R.id.settings_show_advanced:
                item.setChecked(!item.isChecked());
                Settings.getInstance().setShowAdvanced(item.isChecked());
                if (fragment != null) {
                    fragment.updateEntries();
                    fragment.updateValues();
                }
                return true;
            case R.id.settings_show_unstable:
                item.setChecked(!item.isChecked());
                Settings.getInstance().setShowUnstable(item.isChecked());
                if (fragment != null) {
                    fragment.updateEntries();
                    fragment.updateValues();
                }
                return true;
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                TaskStackBuilder.create(this).addNextIntentWithParentStack(upIntent).startActivities();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Settings.getInstance().restoreShowTouches();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Settings.getInstance().applyShowTouches();
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(RecorderService.SETTINGS_OPENED_ACTION);
        startService(intent);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(RecorderService.SETTINGS_CLOSED_ACTION);
        startService(intent);
        super.onBackPressed();
    }

    private void sendBugReport() {
        new ReportBugTask(getApplicationContext(), 1000).execute();
    }

}


