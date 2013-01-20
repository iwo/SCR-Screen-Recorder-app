package com.iwobanas.ScreenRecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class RecorderService extends Service {

    View mWatermark;

    View mRecorderView;

    private void showWatermark() {
         if (mWatermark == null) {
             LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
             mWatermark = inflater.inflate(R.layout.watermark, null);

             WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                     WindowManager.LayoutParams.MATCH_PARENT,
                     WindowManager.LayoutParams.MATCH_PARENT);
             lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
             lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                     | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                     | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
             lp.format = PixelFormat.TRANSLUCENT;
             lp.setTitle(getString(R.string.app_name));

             WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
             windowManager.addView(mWatermark, lp);
         }
    }

    private void showRecorderView() {
        if (mRecorderView == null) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            mRecorderView = inflater.inflate(R.layout.recorder, null);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            );
            lp.format = PixelFormat.TRANSLUCENT;
            lp.setTitle(getString(R.string.app_name));
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

            Button startButton = (Button) mRecorderView.findViewById(R.id.start_button);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
            windowManager.addView(mRecorderView, lp);
        }
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();

        showRecorderView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
