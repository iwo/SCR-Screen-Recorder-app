package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class WindowPinchListener extends WindowDragListener implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "scr_WindowPinchListener";

    private ScaleGestureDetector scaleGestureDetector;
    private Handler handler;

    private View view;
    private View contentView;
    private ViewGroup.MarginLayoutParams contentViewParams;
    private boolean scaling;
    private boolean inWindowTransition;
    private float focusRatioX;
    private float focusRatioY;
    private float width;
    private float height;
    private float minSize;


    WindowPinchListener(Context context, WindowManager.LayoutParams params, float minSize) {
        super(params);
        this.minSize = minSize;
        handler = new Handler();
        scaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (view == null || view != v) {
            view = v;
            contentView = view.findViewById(R.id.content);

            if (contentView == null) {
                Log.e(TAG, "Missing content view container");
                return false;
            }
            contentViewParams = (ViewGroup.MarginLayoutParams) contentView.getLayoutParams();
            if (contentViewParams == null) {
                contentViewParams = new ViewGroup.MarginLayoutParams(params.width, params.height);
            }
        }

        if (inWindowTransition) {
            return false;
        }

        if (!eventOffsetValid(event)) {
            return false;
        }

        scaleGestureDetector.onTouchEvent(event);

        if (inWindowTransition) {
            return false;
        }

        if (scaling) {
            return true;
        }
        return super.onTouch(v, event);
    }

    private boolean eventOffsetValid(MotionEvent event) {
        return Math.abs(screenToViewX(event.getRawX()) - event.getX()) < 10
                && Math.abs(screenToViewY(event.getRawY()) - event.getY()) < 10;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.v(TAG, "Starting scaling");

        boolean dragInterrupted = interruptDrag();
        scaling = true;

        focusRatioX = screenToViewX(detector.getFocusX()) / view.getWidth();
        focusRatioY = screenToViewY(detector.getFocusY()) / view.getHeight();
        width = params.width;
        height = params.height;
        makeWindowFullScreen();

        if (!dragInterrupted) {
            notifyDragStart();
        }
        return true;
    }

    /**
     * Resizing window is extremely slow and triggers some animations.
     * As a workaround we create use full-screen window during resizing
     * and modify a content view within that window.
     */
    private void makeWindowFullScreen() {

        inWindowTransition = true;

        contentViewParams.leftMargin = getViewX();
        contentViewParams.topMargin = getViewY();
        contentViewParams.width = params.width;
        contentViewParams.height = params.height;
        contentView.setLayoutParams(contentViewParams);
        contentView.setVisibility(View.INVISIBLE);

        handler.post(new Runnable() {
            @Override
            public void run() {
                contentView.setVisibility(View.VISIBLE);
                params.x = 0;
                params.y = 0;
                params.gravity = Gravity.LEFT | Gravity.TOP;
                params.width = WindowManager.LayoutParams.MATCH_PARENT;
                params.height = WindowManager.LayoutParams.MATCH_PARENT;
                updateViewLayout(view);
                inWindowTransition = false;
                Log.v(TAG, "Start transition finished");
            }
        });
    }

    private int getViewX() {
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);
        int gravity = params.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (gravity == Gravity.CENTER_HORIZONTAL)
            return frame.centerX() + params.x - view.getWidth() / 2 - frame.left;
        if (gravity == Gravity.RIGHT)
            return frame.right - params.x - view.getWidth() - frame.left;
        return params.x;
    }

    private int getViewY() {
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);
        int gravity = params.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.CENTER_VERTICAL)
            return frame.centerY() + params.y - view.getHeight() / 2 - frame.top;
        if (gravity == Gravity.BOTTOM)
            return frame.bottom - params.y - view.getHeight() - frame.top;
        return params.y;
    }

    private float screenToViewX(float screenX) {
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);
        return screenX - frame.left - getViewX();
    }

    private float screenToViewY(float screenY) {
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);
        return screenY - frame.top - getViewY();
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        if (inWindowTransition) {
            return true;
        }

        float scale = detector.getScaleFactor();

        if (scale > 1.5f) {
            Log.v(TAG, "Ignoring excessive scale update: " + detector.getScaleFactor());
            return true;
        }

        final Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);

        width *= scale;
        height *= scale;

        int w = (int) width;
        int h = (int) height;

        if (w < minSize) {
            w = (int) minSize;
            h = (int) (minSize * height / width);
        }

        if (h < minSize) {
            h = (int) minSize;
            w = (int) (minSize * width / height);
        }

        if (w > frame.width()) {
            w = frame.width();
            h = (int) (frame.width() * height / width);
        }

        if (h > frame.height()) {
            h = frame.height();
            w = (int) (frame.height() * width / height);
        }

        int x = (int) (screenToViewX(detector.getFocusX()) - w * focusRatioX);
        int y = (int) (screenToViewY(detector.getFocusY()) - h * focusRatioY);

        if (x < 0) {
            x = 0;
        } else if ((x + w) > frame.width()) {
            x = frame.width() - w;
        }

        if (y < 0) {
            y = 0;
        }
        if ((y + h) > frame.height()) {
            y = frame.height() - h;
        }

        contentViewParams.leftMargin = x;
        contentViewParams.topMargin = y;
        contentViewParams.width = w;
        contentViewParams.height = h;
        contentView.setLayoutParams(contentViewParams);

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.v(TAG, "Scaling finished");
        shrinkFullScreenWindow();
        scaling = false;
        notifyDragEnd();
    }

    private void shrinkFullScreenWindow() {
        inWindowTransition = true;

        final float originalAlpha = params.alpha;

        params.width = contentViewParams.width;
        params.height = contentViewParams.height;
        params.alpha = 0;
        updateViewLayout(view);

        handler.post(new Runnable() {
            @Override
            public void run() {
                Rect frame = new Rect();
                view.getWindowVisibleDisplayFrame(frame);

                int x = contentViewParams.leftMargin + frame.left;
                int y = contentViewParams.topMargin + frame.top;

                contentViewParams.leftMargin = 0;
                contentViewParams.topMargin = 0;
                contentViewParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                contentViewParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                contentView.setLayoutParams(contentViewParams);

                params.alpha = originalAlpha;
                setGravityAndPosition(view, x, y);
                inWindowTransition = false;
                Log.v(TAG, "End transition finished");
            }
        });
    }
}
