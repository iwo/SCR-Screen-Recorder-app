package com.iwobanas.screenrecorder.settings;

import android.os.Build;

import com.iwobanas.screenrecorder.R;

public class TransformationDialogFragment extends SettingsListDialogFragment<Transformation> {

    @Override
    protected int getTitle() {
        return R.string.settings_transformation;
    }

    @Override
    protected Transformation[] getItems() {
        if (Build.VERSION.SDK_INT < 18) {
            return new Transformation[]{
                    Transformation.GPU,
                    Transformation.CPU
            };
        }
        return new Transformation[]{
                Transformation.OES,
                Transformation.GPU,
                Transformation.CPU
        };
    }

    @Override
    protected Transformation getSelectedItem() {
        return getSettings().getTransformation();
    }

    @Override
    protected void setSelected(Transformation item) {
        getSettings().setTransformation(item);
    }

    @Override
    protected String[] getLabels() {
        if (Build.VERSION.SDK_INT < 18) {
            return new String[]{
                    getString(R.string.settings_transformation_gpu),
                    getString(R.string.settings_transformation_cpu)
            };
        }
        return new String[]{
                getString(R.string.settings_transformation_oes),
                getString(R.string.settings_transformation_gpu),
                getString(R.string.settings_transformation_cpu)
        };
    }
}
