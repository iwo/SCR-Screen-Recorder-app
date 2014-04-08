package com.iwobanas.screenrecorder;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public abstract class AbstractScreenOverlay implements IScreenOverlay {

    private static final String TAG = "scr_AbstractScreenOverlay";

    private Context mContext;

    private View mView;

    private boolean visible = false;

    public AbstractScreenOverlay(Context context) {
        mContext = context;
    }

    protected abstract View createView();

    protected abstract WindowManager.LayoutParams getLayoutParams();

    protected void updateLayoutParams() {
        if (!visible) {
            return;
        }
        try {
            getWindowManager().updateViewLayout(mView, getLayoutParams());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error updating layout", e);
        }
    }

    protected LayoutInflater getLayoutInflater() {
        return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    protected Context getContext() {
        return mContext;
    }

    protected View getView() {
        if (mView == null) {
            mView = createView();
        }
        return mView;
    }

    @Override
    public void show() {
        if (visible) {
            return;
        }

        getWindowManager().addView(getView(), getLayoutParams());
        visible = true;
    }

    private WindowManager getWindowManager() {
        return (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    }

    protected android.view.Display getDefaultDisplay() {
        return getWindowManager().getDefaultDisplay();
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }
        getWindowManager().removeView(mView);
        visible = false;
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
