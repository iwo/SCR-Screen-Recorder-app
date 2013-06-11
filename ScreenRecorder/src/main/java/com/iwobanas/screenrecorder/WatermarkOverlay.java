package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class WatermarkOverlay extends AbstractScreenOverlay {

    private AnimationDrawable blinkAnimation;

    public WatermarkOverlay(Context context) {
        super(context);
    }

    public void start() {
        blinkAnimation.start();
    }

    public void stop() {
        blinkAnimation.stop();
        blinkAnimation.selectDrawable(0);
    }

    @Override
    protected View createView() {
        View view = getLayoutInflater().inflate(R.layout.watermark, null);
        TextView textView = (TextView) view.findViewById(R.id.watermark_text);
        Drawable[] drawables = textView.getCompoundDrawables();
        blinkAnimation = (AnimationDrawable) drawables[0];
        return view;
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
