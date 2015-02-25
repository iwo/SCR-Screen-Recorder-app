package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

class WindowDragListener implements View.OnTouchListener {
    private static final String TAG = "scr_WindowDragListener";
    private int dragStartX;
    private int dragStartY;
    private boolean dragging;
    protected final WindowManager.LayoutParams params;

    private OnWindowDragStartListener startListener;
    private OnWindowDragEndListener endListener;

    WindowDragListener(WindowManager.LayoutParams params) {
        this.params = params;
    }

    /*
     * motionEvent.getY() - coordinates relative to view
     * motionEvent.getRawY() - raw screen coordinates
     * dragStartY - relative coordinates of grab location
     */

    @Override
    public boolean onTouch(final View view, MotionEvent motionEvent) {
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    dragStartX = (int) motionEvent.getX();
                    dragStartY = (int) motionEvent.getY();
                    Log.v(TAG, "Start drag " + dragStartX + ":" +dragStartY);
                    dragging = true;
                    notifyDragStart();
                    return true;
                }
                float x = motionEvent.getRawX() - dragStartX;
                float y = motionEvent.getRawY() - dragStartY;
                return setGravityAndPosition(view, x, y);

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    Log.v(TAG, "Drag end");
                    dragging = false;
                    notifyDragEnd();
                    return true;
                }
                break;
        }
        return false;
    }

    protected boolean setGravityAndPosition(View view, float x, float y) {
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);

        int leftX, topY, centerX, centerY, rightX, bottomY, gravity = 0;

        leftX = (int) (x - frame.left);
        centerX = (int) (x - (frame.centerX() - view.getWidth() / 2));
        rightX = (int) (frame.right - x - view.getWidth());

        topY = (int) (y - frame.top);
        centerY = (int) (y - (frame.centerY() - view.getHeight() / 2));
        bottomY = (int) (frame.bottom - y - view.getHeight());

        if (leftX <= Math.abs(centerX) && leftX <= rightX) {
            params.x = Math.max(leftX, 0);
            gravity |= Gravity.LEFT;
        } else if (rightX <= Math.abs(centerX) && rightX <= leftX) {
            params.x = Math.max(rightX, 0);
            gravity |= Gravity.RIGHT;
        } else {
            params.x = centerX;
            gravity |= Gravity.CENTER_HORIZONTAL;
        }

        if (topY <= Math.abs(centerY) && topY <= bottomY) {
            params.y = Math.max(topY, 0);
            gravity |= Gravity.TOP;
        } else if (bottomY <= Math.abs(centerY) && bottomY <= topY) {
            params.y = Math.max(bottomY, 0);
            gravity |= Gravity.BOTTOM;
        } else {
            params.y = centerY;
            gravity |= Gravity.CENTER_VERTICAL;
        }
        params.gravity = gravity;

        return updateViewLayout(view);
    }

    protected boolean interruptDrag() {
        if (!dragging)
            return false;
        dragging = false;
        return true;
    }

    protected boolean updateViewLayout(View view) {
        try {
            getWindowManager(view.getContext()).updateViewLayout(view, params);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error updating layout", e);
            return false;
        }
        return true;
    }

    protected void notifyDragStart() {
        if (startListener != null) {
            startListener.onDragStart();
        }
    }

    protected void notifyDragEnd() {
        if (endListener != null) {
            endListener.onDragEnd();
        }
    }

    private WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void setDragStartListener(OnWindowDragStartListener startListener) {
        this.startListener = startListener;
    }

    public void setDragEndListener(OnWindowDragEndListener endListener) {
        this.endListener = endListener;
    }

    public interface OnWindowDragStartListener {
        void onDragStart();
    }

    public interface OnWindowDragEndListener {
        void onDragEnd();
    }
}

