package com.iwobanas.screenrecorder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;

public class RecorderOverlay extends AbstractScreenOverlay {
    private static final String RECORDER_OVERLAY = "RECORDER_OVERLAY";

    private IRecorderService service;
    private ImageButton settingsButton;
    private WindowManager.LayoutParams layoutParams;
    private OverlayPositionPersister positionPersister;

    public RecorderOverlay(Context context, IRecorderService service) {
        super(context);
        this.service = service;
    }

    public void highlightPosition() {
        int x = layoutParams.x;
        float delta = Utils.dipToPixels(getContext(), 20f);
        ValueAnimator animator = ValueAnimator.ofInt(x, x + (int)delta, x - (int) (delta * 0.6f), x + (int) (delta * 0.3f), x);
        animator.setDuration(750);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                layoutParams.x = value;
                updateLayoutParams();
            }
        });
        animator.start();
    }

    @Override
    protected View createView() {
        View view = getLayoutInflater().inflate(R.layout.recorder, null);

        ImageButton startButton = (ImageButton) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.startRecording();
            }
        });

        WindowDragListener dragListener = new WindowDragListener(getLayoutParams());
        dragListener.setDragStartListener(new WindowDragListener.OnWindowDragStartListener() {
            @Override
            public void onDragStart() {
                getView().setBackgroundResource(R.drawable.bg_h);
            }
        });
        dragListener.setDragEndListener(new WindowDragListener.OnWindowDragEndListener() {
            @Override
            public void onDragEnd() {
                getView().setBackgroundResource(R.drawable.bg);
            }
        });
        view.setOnTouchListener(dragListener);

        settingsButton = (ImageButton) view.findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.showSettings();
            }
        });

        ImageButton closeButton = (ImageButton) view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.close();
            }
        });
        return view;
    }

    @Override
    protected WindowManager.LayoutParams getLayoutParams() {
        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            );
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.setTitle(getContext().getString(R.string.app_name));
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.y = -200; // offset so that overlay doesn't cover permission dialog
            layoutParams.gravity = Gravity.CENTER;
            positionPersister = new OverlayPositionPersister(getContext(), RECORDER_OVERLAY, layoutParams);
        }
        return layoutParams;
    }

    @Override
    public void onDestroy() {
        if (positionPersister != null) {
            positionPersister.persistPosition();
        }
    }

    @Override
    public void show() {
        if (!isVisible()) {
            getLayoutParams().windowAnimations = 0;
        }
        super.show();
    }

    public void animateShow() {
        if (!isVisible()) {
            getLayoutParams().windowAnimations = android.R.style.Animation_Translucent;
        }
        super.show();
    }

    @Override
    public void hide() {
        setHideAnimation(0);
        super.hide();
    }

    public void animateHide() {
        setHideAnimation(android.R.style.Animation_Translucent);
        super.hide();
    }

    private void setHideAnimation(int animation) {
        if (isVisible() && getLayoutParams().windowAnimations != animation) {
            getLayoutParams().windowAnimations = animation;
            updateLayoutParams();
        }
    }
}
