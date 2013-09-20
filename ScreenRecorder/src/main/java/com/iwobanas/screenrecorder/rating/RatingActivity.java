package com.iwobanas.screenrecorder.rating;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.iwobanas.screenrecorder.RecorderService;

public class RatingActivity extends Activity {

    private static final String DIALOG_DISPLAYED = "DIALOG_DISPLAYED";
    private boolean dialogDisplayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dialogDisplayed = savedInstanceState != null && savedInstanceState.getBoolean(DIALOG_DISPLAYED, false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (dialogDisplayed) {
            Intent intent = new Intent(this, RecorderService.class);
            startService(intent);
            finish();
        } else {
            new RatingDialogFragment().show(getFragmentManager(), "RatingDialog");
            dialogDisplayed = true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(DIALOG_DISPLAYED, dialogDisplayed);
    }
}
