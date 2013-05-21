package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.View;
import android.view.WindowManager;

public class WatermarkOverlay extends AbstractScreenOverlay {

    public WatermarkOverlay(Context context) {
        super(context);
    }

    @Override
    protected View createView() {
        return getLayoutInflater().inflate(R.layout.watermark, null);
    }

    @Override
    protected WindowManager.LayoutParams getLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.setTitle(getContext().getString(R.string.app_name));
        return lp;
    }
}
