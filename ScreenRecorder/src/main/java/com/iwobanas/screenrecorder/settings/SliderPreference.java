package com.iwobanas.screenrecorder.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;

import com.iwobanas.screenrecorder.R;

public class SliderPreference extends DialogPreference {

    private int value;

    private int progress;

    private int min = 0;

    private int max = 100;

    public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SliderPreference, defStyle, 0);
        try {
            min = a.getInt(R.styleable.SliderPreference_min, min);
            max = a.getInt(R.styleable.SliderPreference_max, max);
        } finally {
            a.recycle();
        }
        setDialogLayoutResource(R.layout.slider_preference);
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
    }

    @Override
    protected View onCreateDialogView() {
        View dialogView = super.onCreateDialogView();
        SeekBar seekBar = (SeekBar) dialogView.findViewById(R.id.slider_preference_seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                SliderPreference.this.progress = progress;
                callChangeListener(min + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return dialogView;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
        value = (Integer) defaultValue;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.slider_preference_seekbar);
        seekBar.setProgress(value - min);
        seekBar.setMax(max - min);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult && callChangeListener(min + progress)) {
            setValue(min + progress);
        } else {
            callChangeListener(getValue());
        }
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
