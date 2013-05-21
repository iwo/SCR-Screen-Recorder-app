package com.iwobanas.screenrecorder;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.widget.Toast;

import java.io.File;

public class RecorderService extends Service implements IRecorderService {

    private IScreenOverlay mWatermark = new WatermarkOverlay(this);

    private IScreenOverlay mRecorderOverlay = new RecorderOverlay(this, this);

    private ScreenOffReceiver mScreenOffReceiver = new ScreenOffReceiver(this);

    @Override
    public void startRecording() {
        mRecorderOverlay.hide();
        mWatermark.show();
        mScreenOffReceiver.register(this);
    }

    @Override
    public void stopRecording() {
        mWatermark.hide();
        mRecorderOverlay.show();
        mScreenOffReceiver.unregister(this);
    }

    @Override
    public void openLastFile() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        File file = new File("/sdcard/screenrec.mp4");
        intent.setDataAndType(Uri.fromFile(file), "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show();

        mRecorderOverlay.show();
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
