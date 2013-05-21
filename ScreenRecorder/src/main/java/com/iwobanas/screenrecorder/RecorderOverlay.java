package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class RecorderOverlay implements IScreenOverlay {

    private Context mContext;

    private View mView;

    private IRecorderService mService;

    public RecorderOverlay(Context context, IRecorderService service) {
        mContext = context;
        mService = service;
    }

    @Override
    public void show() {
        if (mView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mView = inflater.inflate(R.layout.recorder, null);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            );
            lp.format = PixelFormat.TRANSLUCENT;
            lp.setTitle(mContext.getString(R.string.app_name));
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

            Button startButton = (Button) mView.findViewById(R.id.start_button);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mService.startRecording();
                }
            });

            Button playButton = (Button) mView.findViewById(R.id.play_button);
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mService.openLastFile();
                }
            });

            WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            windowManager.addView(mView, lp);
        }
    }

    @Override
    public void hide() {

    }
}
