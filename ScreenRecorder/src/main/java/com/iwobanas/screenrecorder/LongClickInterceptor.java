package com.iwobanas.screenrecorder;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/*
* LinearLayout based container which intercepts all touch events after a long click inside any of it's child.
* The child doesn't need to have long clicks enabled.
* Used by {@link RecorderOverlay} to allow grabbing the window by one of buttons inside it.
*
* It'd be more elegant to create composition based solution using listeners but listeners can't intercept events
* so this seems to be the only option.
* */
public class LongClickInterceptor extends LinearLayout {
    private static final int LONG_CLICK = 1;

    private MotionEvent downEvent;
    private boolean intercept = false;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == LONG_CLICK) {
                intercept = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                dispatchTouchEvent(downEvent);
                clearDownEvent();
            }
            return false;
        }
    });
    private float touchSlopSquare = Float.NaN;


    public LongClickInterceptor(Context context) {
        super(context);
    }

    public LongClickInterceptor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LongClickInterceptor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    private float getTouchSlopeSquare() {
        if (Float.isNaN(touchSlopSquare)) {
            float touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
            touchSlopSquare = touchSlop * touchSlop;
        }
        return touchSlopSquare;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (intercept) {
            intercept = false;
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downEvent = MotionEvent.obtainNoHistory(ev);
                handler.removeMessages(LONG_CLICK);
                handler.sendEmptyMessageAtTime(LONG_CLICK, ev.getDownTime() + ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                if (downEvent != null) {
                    float deltaX = downEvent.getRawX() - ev.getRawX();
                    float deltaY = downEvent.getRawY() - ev.getRawY();
                    float distanceSquare = deltaX * deltaX + deltaY * deltaY;
                    downEvent.setLocation(ev.getX(), ev.getY());
                    if (distanceSquare < getTouchSlopeSquare()) {
                        break;
                    }
                    // if distance is greater fall through to cancellation
                }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeMessages(LONG_CLICK);
                clearDownEvent();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeMessages(LONG_CLICK);
                clearDownEvent();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void clearDownEvent() {
        if (downEvent != null) {
            downEvent.recycle();
            downEvent = null;
        }
    }
}
