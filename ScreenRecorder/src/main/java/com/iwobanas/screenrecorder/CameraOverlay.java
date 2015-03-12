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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.iwobanas.screenrecorder.settings.Settings;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraOverlay extends AbstractScreenOverlay implements TextureView.SurfaceTextureListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "scr_FaceOverlay";
    private static final String FACE_OVERLAY = "FACE_OVERLAY";
    private static final String FACE_OVERLAY_WIDTH = "FACE_OVERLAY_WIDTH";
    private static final String FACE_OVERLAY_HEIGHT = "FACE_OVERLAY_HEIGHT";

    private static CameraOverlay activeInstance;

    int screenPortion = 4; // 1/4 of the longer edge of the screen
    int minScreenPortion = 8; // 1/8 of the longer edge of the screen
    private WindowManager.LayoutParams layoutParams;
    private Camera camera;
    private int cameraOrientation;
    private View rootView;
    private TextureView textureView;
    private ProgressBar progressBar;
    private TextView errorText;
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
    private int width;
    private int height;
    private boolean previewStarted = false;
    private boolean frameReceived = false;
    private OverlayPositionPersister positionPersister;
    private Handler uiHandler;
    private Handler cameraHandler;
    private HandlerThread cameraHandlerThread;
    private boolean openingCamera;
    private boolean releaseWhenOpen;

    public CameraOverlay(Context context) {
        super(context);

        uiHandler = new Handler();

        cameraHandlerThread = new HandlerThread("FaceOverlay");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());

        Settings.getInstance().registerOnSharedPreferenceChangeListener(this);
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
            instance.uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    instance.openCamera();
                }
            }, 1000); // wait 1s before reconnecting
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
        this.camera = camera;
        openingCamera = false;

        if (releaseWhenOpen) {
            releaseWhenOpen = false;
            doReleaseCamera();
            return;
        }

        if (camera == null) {
            Log.w(TAG, "No camera received");
            showErrorMessage();
        } else {
            Log.v(TAG, "Camera opened");
            startPreview();
        }
    }

    private void startPreview() {
        if (!previewStarted && camera != null && textureView != null && textureView.isAvailable()) {
            try {
                Camera.Parameters params = camera.getParameters();
                List<Camera.Size> sizes = params.getSupportedPreviewSizes();
                if (sizes == null) {
                    Log.e(TAG, "Camera not initialized correctly");
                    showErrorMessage();
                    return;
                }
                previewSize = selectPreviewSize(sizes);
                updateSurfaceAspectRatio();
                params.setPreviewSize(previewSize.width, previewSize.height);
                camera.setParameters(params);
                updateCameraRotation();
                updateSurfaceSize();
                frameReceived = false;
                showProgressBar();
                camera.setPreviewTexture(textureView.getSurfaceTexture());
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startPreviewAsync(camera);
                    }
                });
                previewStarted = true;
            } catch (Exception e) {
                Log.e(TAG, "Can't set preview display ", e);
                showErrorMessage();
            }
        }
    }

    private void startPreviewAsync(Camera camera) {
        Log.v(TAG, "Starting preview");
        long startTime = System.nanoTime();
        try {
            camera.startPreview();
            Log.v(TAG, "Preview started in " + (System.nanoTime() - startTime) / 1000000 + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Error starting preview", e);
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showErrorMessage();
                }
            });
        }
    }

    private void showErrorMessage() {
        if (errorText != null) {
            errorText.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (errorText != null) {
            errorText.setVisibility(View.GONE);
        }
    }

    private void updateSurfaceAspectRatio() {
        height = width * previewSize.height / previewSize.width;
        persistSize();
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

        for (Camera.Size size : sizes) {
            if (size.width >= width) {
                Log.v(TAG, "Selected preview size: " + size.width + "x" + size.height);
                return size;
            }
        }
        return sizes.get(sizes.size() - 1);
    }

    private void updateSurfaceSize() {
        boolean rotateInDefault = cameraOrientation % 180 == 0;
        boolean rotate = displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180
                ? rotateInDefault : !rotateInDefault;

        layoutParams.width = rotate ? width : height;
        layoutParams.height = rotate ? height : width;
        updateLayoutParams();
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
        initializeSize();
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
            releaseWhenOpen = false;
            Log.w(TAG, "Camera already opening");
            return;
        }
        if (camera != null) {
            Log.w(TAG, "Camera already open");
            return;
        }
        openingCamera = true;

        Log.v(TAG, "Opening camera");
        final int frontFacingCamera = getFrontFacingCamera();

        if (frontFacingCamera < 0) {
            Log.w(TAG, "No front facing camera found!");
            return;
        }
        cameraOrientation = getCameraOrientation(frontFacingCamera);
        cameraHandler.post(new Runnable() {
            @Override
            public void run() {
                openCameraAsync(frontFacingCamera);
            }
        });
    }

    private void openCameraAsync(int cameraId) {
        Log.v(TAG, "Opening camera: " + cameraId);
        long startTime = System.nanoTime();
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
            Log.v(TAG, "Camera opened in " + (System.nanoTime() - startTime) / 1000000 + "ms");
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
        }
        final Camera result = camera;
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                onCameraOpened(result);
            }
        });
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
        if (openingCamera) {
            Log.w(TAG, "Camera release requested before async open completed");
            releaseWhenOpen = true;
            return;
        }
        if (camera == null) return;
        try {
            previewStarted = false;
            camera.stopPreview();
            camera.release();
            camera = null;
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    showProgressBar();
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Error releasing a camera", e);
        }
    }

    @Override
    protected View createView() {
        initializeSize();
        rootView = getLayoutInflater().inflate(R.layout.camera, null);
        if (rootView == null) {
            Log.e(TAG, "Error inflating view");
            return null;
        }
        textureView = (TextureView) rootView.findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
        errorText = (TextView) rootView.findViewById(R.id.error_text);

        Point displaySize = new Point();
        getDefaultDisplay().getSize(displaySize);
        float minSize = Math.max(displaySize.y, displaySize.x) / minScreenPortion;

        WindowPinchListener windowPinchListener = new WindowPinchListener(getContext(), getLayoutParams(), minSize);
        windowPinchListener.setDragEndListener(new WindowDragListener.OnWindowDragEndListener() {
            @Override
            public void onDragEnd() {
                persistSizeAfterScale();
            }
        });
        rootView.setOnTouchListener(windowPinchListener);

        return rootView;
    }

    private void persistSizeAfterScale() {
        if (width > height) {
            width = Math.max(layoutParams.width, layoutParams.height);
            height = Math.min(layoutParams.width, layoutParams.height);
        } else {
            width = Math.min(layoutParams.width, layoutParams.height);
            height = Math.max(layoutParams.width, layoutParams.height);
        }
        persistSize();
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
            initializeSize();
            layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
            layoutParams.format = PixelFormat.TRANSLUCENT;
            layoutParams.setTitle(getContext().getString(R.string.app_name));
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.x = 10;
            layoutParams.y = 10;
            layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
            layoutParams.windowAnimations = 0;
            positionPersister = new OverlayPositionPersister(getContext(), FACE_OVERLAY, layoutParams);
        }
        return layoutParams;
    }

    @Override
    public void onDestroy() {
        persistSize();
        if (positionPersister != null) {
            positionPersister.persistPosition();
        }

        Settings.getInstance().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void initializeSize() {
        if (width > 0 && height > 0)
            return;

        SharedPreferences preferences = getContext().getSharedPreferences(OverlayPositionPersister.SCR_UI_PREFERENCES, Context.MODE_PRIVATE);

        Point displaySize = new Point();
        getDefaultDisplay().getSize(displaySize);
        int defaultWidth = Math.max(displaySize.y, displaySize.x) / screenPortion;
        width = preferences.getInt(FACE_OVERLAY_WIDTH, defaultWidth);
        height = preferences.getInt(FACE_OVERLAY_HEIGHT, (defaultWidth * 3) / 4);
        if (width <= 10 || height <= 10) {
            Log.w(TAG, "Incorrect size previously persisted");
            width = defaultWidth;
            height = (defaultWidth * 3) / 4;
        }
        Log.v(TAG, "Size set to " + width + "x" + height);
    }

    private void persistSize() {
        if (width == 0 || height == 0)
            return;

        getContext().getSharedPreferences(OverlayPositionPersister.SCR_UI_PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putInt(FACE_OVERLAY_WIDTH, width)
                .putInt(FACE_OVERLAY_HEIGHT, height)
                .apply();
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
}
