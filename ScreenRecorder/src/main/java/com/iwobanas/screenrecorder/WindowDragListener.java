package com.iwobanas.screenrecorder;

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

class WindowDragListener implements View.OnTouchListener {
    private float dragStartX;
    private float dragStartY;
    private WindowManager.LayoutParams params;

    WindowDragListener(WindowManager.LayoutParams params) {
        this.params = params;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
            Rect frame = new Rect();
            view.getWindowVisibleDisplayFrame(frame);
            if (params.gravity == (Gravity.TOP | Gravity.LEFT)) {
                dragStartX = motionEvent.getX() + frame.left;
                dragStartY = motionEvent.getY() + frame.top;
            } else {
                dragStartX = motionEvent.getX() + frame.centerX() - view.getWidth() / 2;
                dragStartY = motionEvent.getY() + frame.centerY() - view.getHeight() / 2;
            }

            return true;
        }
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
            params.x = (int) (motionEvent.getRawX() - dragStartX);
            params.y = (int) (motionEvent.getRawY() - dragStartY);

            getWindowManager(view.getContext()).updateViewLayout(view, params);
            return true;
        }
        return false;
    }

    private WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }
}