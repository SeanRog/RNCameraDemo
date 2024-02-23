/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rncamerademo.nativemodules.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import androidx.annotation.NonNull;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.facebook.react.bridge.ReadableMap;

import com.rncamerademo.nativemodules.camera.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;


@SuppressWarnings("MissingPermission")
@TargetApi(21)
class Camera2 {

    private static final String TAG = "rncameranativemodule";
    protected final Callback mCallback;
    protected final TextureViewPreview mPreview;
    protected final Handler mBgHandler;
    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final int FOCUS_AREA_SIZE_DEFAULT = 300;

    private static final int FOCUS_METERING_AREA_WEIGHT_DEFAULT = 1000;

    private final CameraManager mCameraManager;

    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            mCallback.onCameraOpened();
            startCaptureSession();
        }

        public void onClosed(@NonNull CameraDevice camera) {
            mCallback.onCameraClosed();
        }

        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    private final CameraCaptureSession.StateCallback mSessionCallback
            = new CameraCaptureSession.StateCallback() {

        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (mCamera == null) {
                return;
            }
            mCaptureSession = session;
            mInitialCropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            updateAutoFocus();
            updateFlash();
            updateFocusDepth();
            updateWhiteBalance();
            updateZoom();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback, null);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to start camera preview.", e);
            }
        }

        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure capture session.");
        }

        public void onClosed(@NonNull CameraCaptureSession session) {
            if (mCaptureSession != null && mCaptureSession.equals(session)) {
                mCaptureSession = null;
            }
        }

    };

    PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {

        public void onPrecaptureRequired() {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), this, null);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to run precapture sequence.", e);
            }
        }

        public void onReady() {
            captureStillPicture();
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    if (image.getFormat() == ImageFormat.JPEG) {
                        // @TODO: implement deviceOrientation
                        mCallback.onPictureTaken(data, 0, 0);
                    } else {
                        mCallback.onFramePreview(data, image.getWidth(), image.getHeight(), mDisplayOrientation);
                    }
                }
            }
        }

    };


    private String mCameraId;
    private String _mCameraId = "";

    private CameraCharacteristics mCameraCharacteristics;

    CameraDevice mCamera;

    MediaActionSound sound = new MediaActionSound();

    CameraCaptureSession mCaptureSession;

    CaptureRequest.Builder mPreviewRequestBuilder;

    Set<String> mAvailableCameras = new HashSet<>();

    private ImageReader mStillImageReader;

    private ImageReader mScanImageReader;

    private int mImageFormat;

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private Size mPictureSize;

    private int mFacing;

    private AspectRatio mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;

    private AspectRatio mInitialRatio;

    private boolean mAutoFocus;

    private int mFlash;

    private int mCameraOrientation;

    private int mDisplayOrientation;

    private int mDeviceOrientation;

    private float mFocusDepth;

    private float mZoom;

    private int mWhiteBalance;

    private boolean mIsScanning;

    private Boolean mPlaySoundOnCapture = false;

    private Boolean mPlaySoundOnRecord = false;

    private Surface mPreviewSurface;

    private Rect mInitialCropRegion;

    Camera2(Callback callback, TextureViewPreview preview, Context context, Handler bgHandler) {
        mCallback = callback;
        mPreview = preview;
        mBgHandler = bgHandler;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mCameraManager.registerAvailabilityCallback(new CameraManager.AvailabilityCallback() {
            public void onCameraAvailable(@NonNull String cameraId) {
                super.onCameraAvailable(cameraId);
                mAvailableCameras.add(cameraId);
            }

            public void onCameraUnavailable(@NonNull String cameraId) {
                super.onCameraUnavailable(cameraId);
                mAvailableCameras.remove(cameraId);
            }
        }, null);
        mImageFormat = mIsScanning ? ImageFormat.YUV_420_888 : ImageFormat.JPEG;
        mPreview.setCallback(new TextureViewPreview.Callback() {
            public void onSurfaceChanged() {
                startCaptureSession();
            }

            public void onSurfaceDestroyed() {
                stop();
            }
        });
    }

    View getView() {
        return mPreview.getView();
    }

    boolean start() {
        if (!chooseCameraIdByFacing()) {
            mAspectRatio = mInitialRatio;
            mCallback.onMountError();
            return false;
        }
        collectCameraInfo();
        setAspectRatio(mInitialRatio);
        mInitialRatio = null;
        prepareStillImageReader();
        prepareScanImageReader();
        startOpeningCamera();
        return true;
    }

    void stop() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mStillImageReader != null) {
            mStillImageReader.close();
            mStillImageReader = null;
        }

        if (mScanImageReader != null) {
            mScanImageReader.close();
            mScanImageReader = null;
        }
    }

    boolean isCameraOpened() {
        return mCamera != null;
    }

    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    int getFacing() {
        return mFacing;
    }

    public ArrayList<int[]> getSupportedPreviewFpsRange() {
        Log.e("CAMERA_2:: ", "getSupportedPreviewFpsRange is not currently supported for Camera2");
        ArrayList<int[]> validValues = new ArrayList<int[]>();
        return validValues;
    }

    void setCameraId(String id) {
        if(!ObjectUtils.equals(_mCameraId, id)){
            _mCameraId = id;

            // only update if our camera ID actually changes
            // from what we currently have.
            // Passing null will always yield true
            if(!ObjectUtils.equals(_mCameraId, mCameraId)){
                // this will call chooseCameraIdByFacing
                if (isCameraOpened()) {
                    stop();
                    start();
                }
            }
        }
    }

    String getCameraId() {
        return _mCameraId;
    }

    Set<AspectRatio> getSupportedAspectRatios() {
        return mPreviewSizes.ratios();
    }

    List<Properties> getCameraIds() {
        try{

            List<Properties> ids = new ArrayList<>();

            String[] cameraIds = mCameraManager.getCameraIdList();
            for (String id : cameraIds) {
                Properties p = new Properties();

                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);

                p.put("id", id);
                p.put("type", String.valueOf(internal == CameraCharacteristics.LENS_FACING_FRONT ? Constants.FACING_FRONT : Constants.FACING_BACK));
                ids.add(p);
            }
            return ids;
        }
        catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera ids", e);
        }
    }

    SortedSet<Size> getAvailablePictureSizes(AspectRatio ratio) {
        return mPictureSizes.sizes(ratio);
    }

    void setPictureSize(Size size) {
        if (mCaptureSession != null) {
            try {
                mCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mStillImageReader != null) {
            mStillImageReader.close();
        }
        if (size == null) {
          if (mAspectRatio == null || mPictureSize == null) {
            return;
          }
          mPictureSizes.sizes(mAspectRatio).last();
        } else {
          mPictureSize = size;
        }
        prepareStillImageReader();
        startCaptureSession();
    }

    Size getPictureSize() {
        return mPictureSize;
    }

    boolean setAspectRatio(AspectRatio ratio) {
        if (ratio != null && mPreviewSizes.isEmpty()) {
            mInitialRatio = ratio;
            return false;
        }
        if (ratio == null || ratio.equals(mAspectRatio) ||
                !mPreviewSizes.ratios().contains(ratio)) {
            // TODO: Better error handling
            return false;
        }
        mAspectRatio = ratio;
        prepareStillImageReader();
        prepareScanImageReader();
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
            startCaptureSession();
        }
        return true;
    }

    AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        mAutoFocus = autoFocus;
        if (mPreviewRequestBuilder != null) {
            updateAutoFocus();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, null);
                } catch (CameraAccessException e) {
                    mAutoFocus = !mAutoFocus; // Revert
                }
            }
        }
    }

    boolean getAutoFocus() {
        return mAutoFocus;
    }

    void setFlash(int flash) {
        if (mFlash == flash) {
            return;
        }
        int saved = mFlash;
        mFlash = flash;
        if (mPreviewRequestBuilder != null) {
            updateFlash();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, null);
                } catch (CameraAccessException e) {
                    mFlash = saved; // Revert
                }
            }
        }
    }

    int getFlash() {
        return mFlash;
    }


    void takePicture(ReadableMap options) {
        mCaptureCallback.setOptions(options);

        if (mAutoFocus) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }

    public void setFocusDepth(float value) {
        if (mFocusDepth == value) {
            return;
        }
        float saved = mFocusDepth;
        mFocusDepth = value;
        if (mCaptureSession != null) {
            updateFocusDepth();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mFocusDepth = saved;  // Revert
            }
        }
    }

    float getFocusDepth() {
        return mFocusDepth;
    }

    public void setZoom(float zoom) {
      if (mZoom == zoom) {
          return;
      }
      float saved = mZoom;
      mZoom = zoom;
      if (mCaptureSession != null) {
          updateZoom();
          try {
              mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                  mCaptureCallback, null);
          } catch (CameraAccessException e) {
              mZoom = saved;  // Revert
          }
      }
    }

    float getZoom() {
        return mZoom;
    }

    public void setWhiteBalance(int whiteBalance) {
        if (mWhiteBalance == whiteBalance) {
            return;
        }
        int saved = mWhiteBalance;
        mWhiteBalance = whiteBalance;
        if (mCaptureSession != null) {
            updateWhiteBalance();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                    mCaptureCallback, null);
            } catch (CameraAccessException e) {
                mWhiteBalance = saved;  // Revert
            }
        }
    }

    public int getWhiteBalance() {
        return mWhiteBalance;
    }

    void setPlaySoundOnCapture(boolean playSoundOnCapture) {
        mPlaySoundOnCapture = playSoundOnCapture;
    }

    public boolean getPlaySoundOnCapture(){
        return mPlaySoundOnCapture;
    }

    void setPlaySoundOnRecord(boolean playSoundOnRecord) {
        mPlaySoundOnRecord = playSoundOnRecord;
    }

    boolean getPlaySoundOnRecord() {
        return mPlaySoundOnRecord;
    }

    void setScanning(boolean isScanning) {
        if (mIsScanning == isScanning) {
            return;
        }
        mIsScanning = isScanning;
        if (!mIsScanning) {
            mImageFormat = ImageFormat.JPEG;
        } else {
            mImageFormat = ImageFormat.YUV_420_888;
        }
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        startCaptureSession();
    }

    boolean getScanning() {
        return mIsScanning;
    }

    int getCameraOrientation() {
        return mCameraOrientation;
    }

    void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }


    void setDeviceOrientation(int deviceOrientation) {
        mDeviceOrientation = deviceOrientation;
        //mPreview.setDisplayOrientation(deviceOrientation); // this is not needed and messes up the display orientation
    }


    // This is a helper method to query Camera2 legacy status so we don't need
    // to instantiate and set all its props in order to check if it is legacy or not
    // and then fallback to Camera1. This way, legacy devices can fall back to Camera1 right away
    // This method makes sure all cameras are not legacy, so further checks are not needed.
    public static boolean isLegacy(Context context){
        try{
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] ids = manager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer level = characteristics.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    Log.w(TAG, "Camera2 can only run in legacy mode and should not be used.");
                    return true;
                }
            }
            return false;
        }
        catch(CameraAccessException ex){
            Log.e(TAG, "Failed to check camera legacy status, returning true.", ex);
            return true;
        }
    }

    /**
     * <p>Chooses a camera ID by the specified camera facing ({@link #mFacing}).</p>
     * <p>This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and optionally
     * {@link #mFacing}.</p>
     */
    private boolean chooseCameraIdByFacing() {
        if(_mCameraId == null || _mCameraId.isEmpty()){
            try {
                int internalFacing = INTERNAL_FACINGS.get(mFacing);
                final String[] ids = mCameraManager.getCameraIdList();
                if (ids.length == 0) { // No camera
                    Log.e(TAG, "No cameras available.");
                    return false;
                }
                for (String id : ids) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);

                    Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (internal == null) {
                        Log.e(TAG, "Unexpected state: LENS_FACING null");
                        continue;
                    }
                    if (internal == internalFacing) {
                        mCameraId = id;
                        mCameraCharacteristics = characteristics;
                        return true;
                    }
                }
                // Not found
                mCameraId = ids[0];
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);

                Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    Log.e(TAG, "Unexpected state: LENS_FACING null");
                    return false;
                }
                for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                    if (INTERNAL_FACINGS.valueAt(i) == internal) {
                        mFacing = INTERNAL_FACINGS.keyAt(i);
                        return true;
                    }
                }
                // The operation can reach here when the only camera device is an external one.
                // We treat it as facing back.
                mFacing = Constants.FACING_BACK;
                return true;
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to get a list of camera devices", e);
                return false;
            }
        }
        else{

            try{
                // need to set the mCameraCharacteristics variable as above and also do the same checks
                // for legacy hardware
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(_mCameraId);

                // set our facing variable so orientation also works as expected
                Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    Log.e(TAG, "Unexpected state: LENS_FACING null");
                    return false;
                }
                for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                    if (INTERNAL_FACINGS.valueAt(i) == internal) {
                        mFacing = INTERNAL_FACINGS.keyAt(i);
                        break;
                    }
                }

                mCameraId = _mCameraId;
                return true;
            }
            catch(Exception e){
                Log.e(TAG, "Failed to get camera characteristics", e);
                return false;
            }
        }
    }

    /**
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     * <p>This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes},
     * {@link #mCameraOrientation}, and optionally, {@link #mAspectRatio}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }
        mPreviewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                mPreviewSizes.add(new Size(width, height));
            }
        }
        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes, map);
        if (mPictureSize == null) {
            mPictureSize = mPictureSizes.sizes(mAspectRatio).last();
        }
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            if (!mPictureSizes.ratios().contains(ratio)) {
                mPreviewSizes.remove(ratio);
            }
        }

        if (!mPreviewSizes.ratios().contains(mAspectRatio)) {
            mAspectRatio = mPreviewSizes.ratios().iterator().next();
        }

        mCameraOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    protected void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(mImageFormat)) {
            mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
        }
    }

    private void prepareStillImageReader() {
        if (mStillImageReader != null) {
            mStillImageReader.close();
        }
        mStillImageReader = ImageReader.newInstance(mPictureSize.getWidth(), mPictureSize.getHeight(),
                ImageFormat.JPEG, 1);
        mStillImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    private void prepareScanImageReader() {
        if (mScanImageReader != null) {
            mScanImageReader.close();
        }
        Size largest = mPreviewSizes.sizes(mAspectRatio).last();
        mScanImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.YUV_420_888, 1);
        mScanImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    /**
     * <p>Starts opening a camera device.</p>
     * <p>The result will be processed in {@link #mCameraDeviceCallback}.</p>
     */
    private void startOpeningCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mCameraDeviceCallback, null);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to open camera: " + mCameraId, e);
        }
    }

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>This rewrites {@link #mPreviewRequestBuilder}.</p>
     * <p>The result will be continuously processed in {@link #mSessionCallback}.</p>
     */
    void startCaptureSession() {
        if (!isCameraOpened() || !mPreview.isReady() || mStillImageReader == null || mScanImageReader == null) {
            return;
        }
        Size previewSize = chooseOptimalSize();
        mPreview.setBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = getPreviewSurface();
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            if (mIsScanning) {
                mPreviewRequestBuilder.addTarget(mScanImageReader.getSurface());
            }
            mCamera.createCaptureSession(Arrays.asList(surface, mStillImageReader.getSurface(),
                    mScanImageReader.getSurface()), mSessionCallback, null);
//            List<OutputConfiguration> outputConfigurations = Arrays.asList(
//                    new OutputConfiguration(surface),
//                    new OutputConfiguration(mStillImageReader.getSurface()),
//                    new OutputConfiguration(mScanImageReader.getSurface()));
//
//            mCamera.createCaptureSession(
//                    new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
//                            outputConfigurations,
//                            runnable -> Log.d(TAG, "running executor"),
//                            mSessionCallback));
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to start capture session", e);
            mCallback.onMountError();
        }
    }

    public void resumePreview() {
        unlockFocus();
    }

    public void pausePreview() {
        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public Surface getPreviewSurface() {
        if (mPreviewSurface != null) {
            return mPreviewSurface;
        }
        return mPreview.getSurface();
    }

    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (surfaceTexture != null) {
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewSurface = previewSurface;
        } else {
            mPreviewSurface = null;
        }

        // it may be called from another thread, so make sure we're in main looper
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mCaptureSession != null) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                startCaptureSession();
            }
        });
    }

    public Size getPreviewSize() {
        return new Size(mPreview.getWidth(), mPreview.getHeight());
    }

    /**
     * Chooses the optimal preview size based on {@link #mPreviewSizes} and the surface size.
     *
     * @return The picked size for camera preview.
     */
    private Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
    }

    /**
     * Updates the internal state of auto-focus to {@link #mAutoFocus}.
     */
    void updateAutoFocus() {
        if (mAutoFocus) {
            int[] modes = mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    /**
     * Updates the internal state of flash to {@link #mFlash}.
     */
    void updateFlash() {
        switch (mFlash) {
            case Constants.FLASH_OFF:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_ON:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_TORCH:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_RED_EYE:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
        }
    }

    /**
     * Updates the internal state of focus depth to {@link #mFocusDepth}.
     */
    void updateFocusDepth() {
        if (mAutoFocus) {
          return;
        }
        Float minimumLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (minimumLens == null) {
          throw new NullPointerException("Unexpected state: LENS_INFO_MINIMUM_FOCUS_DISTANCE null");
        }
        float value = mFocusDepth * minimumLens;
        mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, value);
    }

    /**
     * Updates the internal state of zoom to {@link #mZoom}.
     */
    void updateZoom() {
        float maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        float scaledZoom = mZoom * (maxZoom - 1.0f) + 1.0f;
        Rect currentPreview = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (currentPreview != null) {
            int currentWidth = currentPreview.width();
            int currentHeight = currentPreview.height();
            int zoomedWidth = (int) (currentWidth / scaledZoom);
            int zoomedHeight = (int) (currentHeight / scaledZoom);
            int widthOffset = (currentWidth - zoomedWidth) / 2;
            int heightOffset = (currentHeight - zoomedHeight) / 2;

            Rect zoomedPreview = new Rect(
                currentPreview.left + widthOffset,
                currentPreview.top + heightOffset,
                currentPreview.right - widthOffset,
                currentPreview.bottom - heightOffset
            );

            // ¯\_(ツ)_/¯ for some devices calculating the Rect for zoom=1 results in a bit different
            // Rect that device claims as its no-zoom crop region and the preview freezes
            if (scaledZoom != 1.0f) {
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomedPreview);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mInitialCropRegion);
            }
        }
    }

    /**
     * Updates the internal state of white balance to {@link #mWhiteBalance}.
     */
    void updateWhiteBalance() {
        switch (mWhiteBalance) {
            case Constants.WB_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO);
                break;
            case Constants.WB_CLOUDY:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
                break;
            case Constants.WB_FLUORESCENT:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
                break;
            case Constants.WB_INCANDESCENT:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT);
                break;
            case Constants.WB_SHADOW:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_SHADE);
                break;
            case Constants.WB_SUNNY:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
                break;
        }
    }

    /**
     * Locks the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to lock focus.", e);
        }
    }


    /**
     * Auto focus on input coordinates
     */

    // Much credit - https://gist.github.com/royshil/8c760c2485257c85a11cafd958548482
    void setFocusArea(float x, float y) {
        if (mCaptureSession == null) {
            return;
        }
        CameraCaptureSession.CaptureCallback captureCallbackHandler = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);

                if (request.getTag() == "FOCUS_TAG") {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to manual focus.", e);
                    }
                }
            }

            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                super.onCaptureFailed(session, request, failure);
                Log.e(TAG, "Manual AF failure: " + failure);
            }
        };

        try {
            mCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to manual focus.", e);
        }

        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), captureCallbackHandler, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to manual focus.", e);
        }

        if (isMeteringAreaAFSupported()) {
            MeteringRectangle focusAreaTouch = calculateFocusArea(x, y);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaTouch});
        }
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mPreviewRequestBuilder.setTag("FOCUS_TAG");

        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), captureCallbackHandler, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to manual focus.", e);
        }
    }

    private boolean isMeteringAreaAFSupported() {
        return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1;
    }

    private MeteringRectangle calculateFocusArea(float x, float y) {
        final Rect sensorArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        // Current iOS spec has a requirement on sensor orientation that doesn't change, spec followed here.
        final int xCoordinate = (int)(y  * (float)sensorArraySize.height());
        final int yCoordinate = (int)(x * (float)sensorArraySize.width());
        final int halfTouchWidth  = 150;  //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
        final int halfTouchHeight = 150;
        MeteringRectangle focusAreaTouch = new MeteringRectangle(Math.max(yCoordinate - halfTouchWidth,  0),
                                                                Math.max(xCoordinate - halfTouchHeight, 0),
                                                                halfTouchWidth  * 2,
                                                                halfTouchHeight * 2,
                                                                MeteringRectangle.METERING_WEIGHT_MAX - 1);

        return focusAreaTouch;
    }

    /**
     * Captures a still picture.
     */
    void captureStillPicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            if (mIsScanning) {
                mImageFormat = ImageFormat.JPEG;
                captureRequestBuilder.removeTarget(mScanImageReader.getSurface());
            }
            captureRequestBuilder.addTarget(mStillImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            switch (mFlash) {
                case Constants.FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    break;
                case Constants.FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case Constants.FLASH_TORCH:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    break;
                case Constants.FLASH_AUTO:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case Constants.FLASH_RED_EYE:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOutputRotation());


            if(mCaptureCallback.getOptions().hasKey("quality")){
                int quality = (int) (mCaptureCallback.getOptions().getDouble("quality") * 100);
                captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte)quality);
            }

            captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION));
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                @NonNull CaptureRequest request,
                                @NonNull TotalCaptureResult result) {
                            if (mCaptureCallback.getOptions().hasKey("pauseAfterCapture")
                              && !mCaptureCallback.getOptions().getBoolean("pauseAfterCapture")) {
                                unlockFocus();
                            }
                            if (mPlaySoundOnCapture) {
                                sound.play(MediaActionSound.SHUTTER_CLICK);
                            }
                        }
                    }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot capture a still picture.", e);
        }
    }

    private int getOutputRotation() {
        @SuppressWarnings("ConstantConditions")
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // updated and copied from Camera1
        if (mFacing == Constants.FACING_BACK) {
           return (sensorOrientation + mDeviceOrientation) % 360;
        } else {
            final int landscapeFlip = isLandscape(mDeviceOrientation) ? 180 : 0;
            return (sensorOrientation + mDeviceOrientation + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == Constants.LANDSCAPE_90 ||
                orientationDegrees == Constants.LANDSCAPE_270);
    }

    /**
     * Unlocks the auto-focus and restart camera preview. This is supposed to be called after
     * capturing a still picture.
     */
    void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
            updateAutoFocus();
            updateFlash();
            if (mIsScanning) {
                mImageFormat = ImageFormat.YUV_420_888;
                startCaptureSession();
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                        null);
                mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to restart camera preview.", e);
        }
    }

    interface Callback {

        void onCameraOpened();

        void onCameraClosed();

        void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation);

        void onFramePreview(byte[] data, int width, int height, int orientation);

        void onMountError();
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} for capturing a still picture.
     */
    private static abstract class PictureCaptureCallback
            extends CameraCaptureSession.CaptureCallback {

        static final int STATE_PREVIEW = 0;
        static final int STATE_LOCKING = 1;
        static final int STATE_LOCKED = 2;
        static final int STATE_PRECAPTURE = 3;
        static final int STATE_WAITING = 4;
        static final int STATE_CAPTURING = 5;

        private int mState;
        private ReadableMap mOptions = null;

        PictureCaptureCallback() {
        }

        void setState(int state) {
            mState = state;
        }

        void setOptions(ReadableMap options) { mOptions = options; }

        ReadableMap getOptions() { return mOptions; }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

        private void process(@NonNull CaptureResult result) {
            switch (mState) {
                case STATE_LOCKING: {
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (af == null) {
                        break;
                    }
                    if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setState(STATE_CAPTURING);
                            onReady();
                        } else {
                            setState(STATE_LOCKED);
                            onPrecaptureRequired();
                        }
                    }
                    break;
                }
                case STATE_PRECAPTURE: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_WAITING);
                    }
                    break;
                }
                case STATE_WAITING: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setState(STATE_CAPTURING);
                        onReady();
                    }
                    break;
                }
            }
        }

        /**
         * Called when it is ready to take a still picture.
         */
        public abstract void onReady();

        /**
         * Called when it is necessary to run the precapture sequence.
         */
        public abstract void onPrecaptureRequired();

    }

}
