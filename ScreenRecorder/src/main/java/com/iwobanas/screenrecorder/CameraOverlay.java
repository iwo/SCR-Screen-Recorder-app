package com.iwobanas.screenrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;

import com.iwobanas.screenrecorder.settings.Settings;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraOverlay extends AbstractScreenOverlay implements TextureView.SurfaceTextureListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "scr_FaceOverlay";
    private static final String FACE_OVERLAY = "FACE_OVERLAY";
    private static final int MSG_OPEN_CAMERA = 1;
    private static final int MSG_SHOW_PROGRESS_BAR = 2;

    private static CameraOverlay activeInstance;

    int screenPortion = 4; // 1/4 of the longer edge of the screen
    private WindowManager.LayoutParams layoutParams;
    private Camera camera;
    private int cameraOrientation;
    private View rootView;
    private TextureView textureView;
    private ProgressBar progressBar;
    private Camera.Size previewSize;
    private int displayRotation = -1;
    private BroadcastReceiver configChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int rotation = getDefaultDisplay().getRotation();
            if (rotation != displayRotation) {
                displayRotation = rotation;
                updateCameraRotation();
                updateSurfaceSize();
            }
        }
    };
    private int width = 160;
    private int height = 120;
    private boolean previewStarted = false;
    private boolean frameReceived = false;
    private OverlayPositionPersister positionPersister;
    private Handler handler;
    private boolean openingCamera;

    public CameraOverlay(Context context) {
        super(context);
        Settings.getInstance().registerOnSharedPreferenceChangeListener(this);
        handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_OPEN_CAMERA:
                        openCamera();
                        break;
                    case MSG_SHOW_PROGRESS_BAR:
                        if (progressBar != null) {
                            progressBar.setVisibility(View.VISIBLE);
                        }
                }
            }
        };
    }

    public static void releaseCamera() {
        CameraOverlay instance = activeInstance;
        if (instance != null) {
            instance.doReleaseCamera();
        }
    }

    public static void reconnectCamera() {
        final CameraOverlay instance = activeInstance;
        if (instance != null) {
            instance.handler.sendEmptyMessageDelayed(MSG_OPEN_CAMERA, 1000); // wait 1s before reconnecting
        }
    }

    public void applySettings() {
        Settings s = Settings.getInstance();

        if (s.getShowCamera()) {
            show();
        } else {
            hide();
        }
    }

    public void setTouchable(boolean touchable) {
        if (touchable) {
            getLayoutParams().flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            getLayoutParams().flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        updateLayoutParams();
    }

    private void onCameraOpened(Camera camera) {
        if (camera == null) {
            Log.w(TAG, "No camera received");
        } else {
            Log.v(TAG, "Camera opened");
        }
        this.camera = camera;
        openingCamera = false;

        getDefaultDisplay().getRotation();
        startPreview();
    }

    private void startPreview() {
        if (!previewStarted && camera != null && textureView != null && textureView.isAvailable()) {
            try {
                Camera.Parameters params = camera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                if (sizes == null) {
                    Log.e(TAG, "Camera not initialized correctly");
                    return;
                }
                previewSize = selectPreviewSize(sizes);
                initializeSurfaceSize();
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
                updateCameraRotation();
                updateSurfaceSize();
                frameReceived = false;
                progressBar.setVisibility(View.VISIBLE);
                camera.setPreviewTexture(textureView.getSurfaceTexture());
                camera.startPreview();
                previewStarted = true;
            } catch (Exception e) {
                Log.e(TAG, "Can't set preview display ", e);
            }
        }
    }

    private void initializeSurfaceSize() {
        Point displaySize = new Point();
        getDefaultDisplay().getSize(displaySize);

        width = Math.max(displaySize.y, displaySize.x) / screenPortion;
        height = width * previewSize.height / previewSize.width;
    }

    private Camera.Size selectPreviewSize(List<Camera.Size> sizes) {
        if (sizes.size() == 1) {
            return sizes.get(0);
        }
        // sort by width asc, height desc
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width == rhs.width) {
                    return rhs.height - lhs.height;
                }
                return lhs.width - rhs.width;
            }
        });
        Point displaySize = new Point();
        getDefaultDisplay().getSize(displaySize);

        int targetWidth = Math.max(displaySize.y, displaySize.x) / screenPortion;

        for (Camera.Size size : sizes) {
            if (size.width >= targetWidth) {
                Log.v(TAG, "Selected preview size: " + size.width + "x" + size.height);
                return size;
            }
        }
        return sizes.get(sizes.size() - 1);
    }

    private void updateSurfaceSize() {
        if (textureView == null) return;
        ViewGroup.LayoutParams lp = textureView.getLayoutParams();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(1, 1);
        }
        boolean rotateInDefault = cameraOrientation % 180 == 0;
        boolean rotate = displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180
                ? rotateInDefault : !rotateInDefault;

        lp.width = rotate ? width : height;
        lp.height = rotate ? height : width;

        textureView.setLayoutParams(lp);
    }

    private void updateCameraRotation() {
        if (camera == null) return;
        int degrees = 0;
        switch (displayRotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        degrees = (cameraOrientation + degrees) % 360;
        degrees = (360 - degrees) % 360;  // compensate the mirror
        camera.setDisplayOrientation(degrees);
    }

    @Override
    public void show() {
        if (!isVisible()) {
            getContext().registerReceiver(configChangeReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
            displayRotation = getDefaultDisplay().getRotation();

            activeInstance = this;
            openCamera();
        }
        super.show();
        if (textureView != null) {
            textureView.setAlpha(Settings.getInstance().getCameraAlpha());
        }
    }

    private void openCamera() {
        if (openingCamera) {
            Log.w(TAG, "Camera already opening");
            return;
        }
        if (camera != null) {
            Log.w(TAG, "Camera already open");
            return;
        }

        Log.v(TAG, "Opening camera");
        int frontFacingCamera = getFrontFacingCamera();

        if (frontFacingCamera < 0) {
            Log.w(TAG, "No front facing camera found!");
            return;
        }
        cameraOrientation = getCameraOrientation(frontFacingCamera);
        new OpenCameraAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frontFacingCamera);
    }

    @Override
    public void hide() {
        if (isVisible()) {
            doReleaseCamera();
            activeInstance = null;
            getContext().unregisterReceiver(configChangeReceiver);
            if (textureView != null) {
                // if alpha is not set to 1 before removing it behaves strangely after re-adding
                textureView.setAlpha(1.0f);
            }
        }
        super.hide();
    }

    private synchronized void doReleaseCamera() {
        if (camera == null) return;
        try {
            previewStarted = false;
            camera.stopPreview();
            camera.release();
            camera = null;
            handler.sendEmptyMessage(MSG_SHOW_PROGRESS_BAR);
        } catch (Exception e) {
            Log.w(TAG, "Error releasing a camera", e);
        }
    }

    @Override
    protected View createView() {
        rootView = getLayoutInflater().inflate(R.layout.camera, null);
        if (rootView == null) {
            Log.e(TAG, "Error inflating view");
            return null;
        }
        textureView = (TextureView) rootView.findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);

        rootView.setOnTouchListener(new WindowDragListener(getLayoutParams()));

        return rootView;
    }

    private int getFrontFacingCamera() {
        int n = Camera.getNumberOfCameras();

        for (int i = 0; i < n; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.v(TAG, "Front facing camera: " + i);
                return i;
            } else {
                Log.v(TAG, "Back facing camera: " + i);
            }
        }
        return -1;
    }

    private int getCameraOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info.orientation;
    }

    @Override
    protected WindowManager.LayoutParams getLayoutParams() {
        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.setTitle(getContext().getString(R.string.app_name));
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.x = 10;
            layoutParams.y = 10;
            layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
            positionPersister = new OverlayPositionPersister(getContext(), FACE_OVERLAY, layoutParams);
        }
        return layoutParams;
    }

    @Override
    public void onDestroy() {
        if (positionPersister != null) {
            positionPersister.persistPosition();
        }

        Settings.getInstance().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.v(TAG, "onSurfaceTextureAvailable");
        startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (!frameReceived) {
            frameReceived = true;
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Settings.SHOW_CAMERA.equals(key) || Settings.CAMERA_ALPHA.equals(key)) {
            applySettings();
        }
    }

    private class OpenCameraAsyncTask extends AsyncTask<Integer, Void, Camera> {

        @Override
        protected Camera doInBackground(Integer... integers) {
            int cameraId = integers[0];
            Log.v(TAG, "Opening camera: " + cameraId);
            try {
                return Camera.open(cameraId);
            } catch (Exception e) {
                Log.e(TAG, "Error opening camera", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Camera camera) {
            super.onPostExecute(camera);

            onCameraOpened(camera);
        }
    }
}
