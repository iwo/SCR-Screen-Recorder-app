package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class RecorderOverlay extends AbstractScreenOverlay {

    private IRecorderService mService;

    public RecorderOverlay(Context context, IRecorderService service) {
        super(context);
        mService = service;
    }

    @Override
    protected View createView() {
        View view = getLayoutInflater().inflate(R.layout.recorder, null);

        Button startButton = (Button) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.startRecording();
            }
        });

        Button playButton = (Button) view.findViewById(R.id.play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mService.openLastFile();
            }
        });
        return view;
    }

    @Override
    protected WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        );
        lp.format = PixelFormat.TRANSLUCENT;
        lp.setTitle(getContext().getString(R.string.app_name));
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        return lp;
    }
}
