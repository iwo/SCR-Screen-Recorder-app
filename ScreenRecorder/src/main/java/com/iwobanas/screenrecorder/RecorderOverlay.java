package com.iwobanas.screenrecorder;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

public class RecorderOverlay extends AbstractScreenOverlay {
    private static final String TAG = "scr_RecorderOverlay";
    private static final String RECORDER_OVERLAY_POSITION_X = "RECORDER_OVERLAY_POSITION_X";
    private static final String RECORDER_OVERLAY_POSITION_Y = "RECORDER_OVERLAY_POSITION_Y";
    private static final String SCR_UI_PREFERENCES = "scr_ui";
    private IRecorderService mService;

    private ImageButton mSettingsButton;
    private WindowManager.LayoutParams layoutParams;

    public RecorderOverlay(Context context, IRecorderService service) {
        super(context);
        mService = service;
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    protected View createView() {
        View view = getLayoutInflater().inflate(R.layout.recorder, null);

        ImageButton startButton = (ImageButton) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.startRecording();
            }
        });

        view.setOnTouchListener(new WindowDragListener(getLayoutParams()));

        mSettingsButton = (ImageButton) view.findViewById(R.id.settings_button);
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.showSettings();
            }
        });

        ImageButton closeButton = (ImageButton) view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.close();
            }
        });
        return view;
    }

    @Override
    protected WindowManager.LayoutParams getLayoutParams() {
        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            );
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.setTitle(getContext().getString(R.string.app_name));
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            SharedPreferences preferences = getContext().getSharedPreferences(SCR_UI_PREFERENCES, Context.MODE_PRIVATE);
            layoutParams.x = preferences.getInt(RECORDER_OVERLAY_POSITION_X, 0);
            layoutParams.y = preferences.getInt(RECORDER_OVERLAY_POSITION_Y, 0);
            Log.v(TAG, "Initializing window position to " + layoutParams.x + ":" + layoutParams.y);
        }
        return layoutParams;
    }

    @Override
    public void onDestroy() {
        if (layoutParams != null) {
            SharedPreferences preferences = getContext().getSharedPreferences(SCR_UI_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(RECORDER_OVERLAY_POSITION_X, layoutParams.x);
            editor.putInt(RECORDER_OVERLAY_POSITION_Y, layoutParams.y);
            editor.commit();
        }
    }
}
