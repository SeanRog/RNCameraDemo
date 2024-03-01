package com.rncamerademo.nativemodules.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.os.AsyncTask;
import android.widget.FrameLayout;

import com.facebook.react.bridge.*;
import com.facebook.react.uimanager.ThemedReactContext;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;

import com.rncamerademo.nativemodules.camera.tasks.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RNCameraView extends FrameLayout implements LifecycleEventListener, FaceDetectorAsyncTaskDelegate,
    TextRecognizerAsyncTaskDelegate, PictureSavedDelegate {
  private static final String TAG = "rncameranativemodule";

  /** The camera device faces the opposite direction as the device's screen. */
  public static final int FACING_BACK = Constants.FACING_BACK;

  /** The camera device faces the same direction as the device's screen. */
  public static final int FACING_FRONT = Constants.FACING_FRONT;

  /** Direction the camera faces relative to device screen. */
  @IntDef({FACING_BACK, FACING_FRONT})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Facing {
  }

  /** Flash will not be fired. */
  public static final int FLASH_OFF = Constants.FLASH_OFF;

  /** Flash will always be fired during snapshot. */
  public static final int FLASH_ON = Constants.FLASH_ON;

  /** Constant emission of light during preview, auto-focus and snapshot. */
  public static final int FLASH_TORCH = Constants.FLASH_TORCH;

  /** Flash will be fired automatically when required. */
  public static final int FLASH_AUTO = Constants.FLASH_AUTO;

  /** Flash will be fired in red-eye reduction mode. */
  public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

  /** The mode for for the camera device's flash control */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
  public @interface Flash {
  }

  Camera2 mCamera2;

  private final CallbackBridge mCallbacks;

  private ReactContext mThemedReactContext;

  protected HandlerThread mBgThread;

  protected Handler mBgHandler;
  private Queue<Promise> mPictureTakenPromises = new ConcurrentLinkedQueue<>();
  private Map<Promise, ReadableMap> mPictureTakenOptions = new ConcurrentHashMap<>();
  private Map<Promise, File> mPictureTakenDirectories = new ConcurrentHashMap<>();

  private ScaleGestureDetector mScaleGestureDetector;
  private GestureDetector mGestureDetector;


  private boolean mIsPaused = false;
  private boolean mIsNew = true;
  private boolean mUseNativeZoom=false;

  public volatile boolean faceDetectorTaskLock = false;
  public volatile boolean googleBarcodeDetectorTaskLock = false;
  public volatile boolean textRecognizerTaskLock = false;

  // Scanning-related properties
  private RNFaceDetector mFaceDetector;
  private boolean mShouldDetectFaces = false;
  private boolean mShouldGoogleDetectBarcodes = false;
  private boolean mShouldScanBarCodes = false;
  private boolean mShouldRecognizeText = false;
  private boolean mShouldDetectTouches = false;
  private int mFaceDetectorMode = RNFaceDetector.FAST_MODE;
  private int mFaceDetectionLandmarks = RNFaceDetector.NO_LANDMARKS;
  private int mFaceDetectionClassifications = RNFaceDetector.NO_CLASSIFICATIONS;
  private boolean mTrackingEnabled = true;

  private static RNCameraView rnCameraView;

  public RNCameraView(ReactContext themedReactContext) {
    super(themedReactContext, null, 0);
    mThemedReactContext = themedReactContext;
    themedReactContext.addLifecycleEventListener(this);

    // bg hanadler for non UI heavy work
    mBgThread = new HandlerThread("RNCamera-Handler-Thread");
    mBgThread.start();
    mBgHandler = new Handler(mBgThread.getLooper());
    final TextureViewPreview preview = createPreview(mThemedReactContext);
    mCallbacks = new CallbackBridge();

    mCamera2 = new Camera2(mCallbacks, preview, mThemedReactContext, mBgHandler);


    addCallback(new Callback() {
      public void onCameraOpened(RNCameraView rnCameraView) {
        RNCameraViewHelper.emitCameraReadyEvent(rnCameraView);
      }

      public void onMountError(RNCameraView cameraView) {
        RNCameraViewHelper.emitMountErrorEvent(cameraView, "Camera view threw an error - component could not be rendered.");
      }

      public void onPictureTaken(RNCameraView cameraView, final byte[] data, int deviceOrientation, int softwareRotation) {
        Promise promise = mPictureTakenPromises.poll();
        ReadableMap options = mPictureTakenOptions.remove(promise);
        if (options.hasKey("fastMode") && options.getBoolean("fastMode")) {
            promise.resolve(null);
        }
        final File cacheDirectory = mPictureTakenDirectories.remove(promise);
        if(Build.VERSION.SDK_INT >= 11/*HONEYCOMB*/) {
          new ResolveTakenPictureAsyncTask(data, promise, options, cacheDirectory, deviceOrientation, softwareRotation, RNCameraView.this)
                  .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
          new ResolveTakenPictureAsyncTask(data, promise, options, cacheDirectory, deviceOrientation, softwareRotation, RNCameraView.this)
                  .execute();
        }
        RNCameraViewHelper.emitPictureTakenEvent(cameraView);
      }
    });
  }

  public static RNCameraView getInstance(ReactContext themedReactContext) {
    if (rnCameraView == null) {
      rnCameraView = new RNCameraView(themedReactContext);
    }
    return rnCameraView;
  }

  public SortedSet<Size> getAvailablePictureSizes(@NonNull AspectRatio ratio) {
    return mCamera2.getAvailablePictureSizes(ratio);
  }

  public List<Properties> getCameraIds() {
    return mCamera2.getCameraIds();
  }

  public void addCallback(@NonNull Callback callback) {
    mCallbacks.add(callback);
  }

  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    View preview = getView();
    if (null == preview) {
      return;
    }
    float width = right - left;
    float height = bottom - top;
    float ratio = mCamera2.getAspectRatio().toFloat();
    int orientation = getResources().getConfiguration().orientation;
    int correctHeight;
    int correctWidth;
    this.setBackgroundColor(Color.BLACK);
    if (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
      if (ratio * height < width) {
        correctHeight = (int) (width / ratio);
        correctWidth = (int) width;
      } else {
        correctWidth = (int) (height * ratio);
        correctHeight = (int) height;
      }
    } else {
      if (ratio * width > height) {
        correctHeight = (int) (width * ratio);
        correctWidth = (int) width;
      } else {
        correctWidth = (int) (height / ratio);
        correctHeight = (int) height;
      }
    }
    int paddingX = (int) ((width - correctWidth) / 2);
    int paddingY = (int) ((height - correctHeight) / 2);
    preview.layout(paddingX, paddingY, correctWidth + paddingX, correctHeight + paddingY);
  }

  public View getView() {
    if (mCamera2 != null) {
      return mCamera2.getView();
    }
    return null;
  }

  public void setFacing(@Facing int facing) {
    mCamera2.setFacing(facing);
  }

  public void setCameraId(String id) {
    mCamera2.setCameraId(id);
  }

  public void setAspectRatio(@NonNull AspectRatio ratio) {
    if (mCamera2.setAspectRatio(ratio)) {
      requestLayout();
    }
  }

  public void setFlash(@Flash int flash) {
    mCamera2.setFlash(flash);
  }

  public ArrayList<int[]> getSupportedPreviewFpsRange() {
    return mCamera2.getSupportedPreviewFpsRange();
  }

  public void setAutoFocus(boolean autoFocus) {
    mCamera2.setAutoFocus(autoFocus);
  }

  public void setFocusDepth(float value) {
    mCamera2.setFocusDepth(value);
  }

  public void setAutoFocusPointOfInterest(float x, float y) {
    mCamera2.setFocusArea(x, y);
  }

  public void setZoom(float zoom) {
    mCamera2.setZoom(zoom);
  }

  public void setWhiteBalance(int whiteBalance) {
    mCamera2.setWhiteBalance(whiteBalance);
  }

  public void setPictureSize(@NonNull Size size) {
    mCamera2.setPictureSize(size);
  }

  @SuppressLint("all")
  public void requestLayout() {
    // React handles this for us, so we don't need to call super.requestLayout();
  }

  public void takePicture(final ReadableMap options, final Promise promise, final File cacheDirectory) {
    mBgHandler.post(() -> {
      mPictureTakenPromises.add(promise);
      mPictureTakenOptions.put(promise, options);
      mPictureTakenDirectories.put(promise, cacheDirectory);

      try {
        mCamera2.takePicture(options);
      } catch (Exception e) {
        mPictureTakenPromises.remove(promise);
        mPictureTakenOptions.remove(promise);
        mPictureTakenDirectories.remove(promise);

        promise.reject("E_TAKE_PICTURE_FAILED", e.getMessage());
      }
    });
  }

  public void onPictureSaved(WritableMap response) {
    RNCameraViewHelper.emitPictureSavedEvent(this, response);
  }


  public void setShouldDetectTouches(boolean shouldDetectTouches) {
    if(!mShouldDetectTouches && shouldDetectTouches){
      mGestureDetector=new GestureDetector(mThemedReactContext,onGestureListener);
    }else{
      mGestureDetector=null;
    }
    this.mShouldDetectTouches = shouldDetectTouches;
  }

  public void setUseNativeZoom(boolean useNativeZoom){
    if(!mUseNativeZoom && useNativeZoom){
      mScaleGestureDetector = new ScaleGestureDetector(mThemedReactContext,onScaleGestureListener);
    }else{
      mScaleGestureDetector=null;
    }
    mUseNativeZoom=useNativeZoom;
  }

  public boolean onTouchEvent(MotionEvent event) {
    if(mUseNativeZoom) {
      mScaleGestureDetector.onTouchEvent(event);
    }
    if(mShouldDetectTouches){
      mGestureDetector.onTouchEvent(event);
    }
    return true;
  }

  /**
   * Initial setup of the face detector
   */
  private void setupFaceDetector() {
    mFaceDetector = new RNFaceDetector(mThemedReactContext);
    mFaceDetector.setMode(mFaceDetectorMode);
    mFaceDetector.setLandmarkType(mFaceDetectionLandmarks);
    mFaceDetector.setClassificationType(mFaceDetectionClassifications);
    mFaceDetector.setTracking(mTrackingEnabled);
  }

  public void setFaceDetectionLandmarks(int landmarks) {
    mFaceDetectionLandmarks = landmarks;
    if (mFaceDetector != null) {
      mFaceDetector.setLandmarkType(landmarks);
    }
  }

  public void setFaceDetectionClassifications(int classifications) {
    mFaceDetectionClassifications = classifications;
    if (mFaceDetector != null) {
      mFaceDetector.setClassificationType(classifications);
    }
  }

  public void setFaceDetectionMode(int mode) {
    mFaceDetectorMode = mode;
    if (mFaceDetector != null) {
      mFaceDetector.setMode(mode);
    }
  }

  public void setTracking(boolean trackingEnabled) {
    mTrackingEnabled = trackingEnabled;
    if (mFaceDetector != null) {
      mFaceDetector.setTracking(trackingEnabled);
    }
  }

  public void onFacesDetected(WritableArray data) {
    if (!mShouldDetectFaces) {
      return;
    }

    RNCameraViewHelper.emitFacesDetectedEvent(this, data);
  }

  public void onFaceDetectionError(RNFaceDetector faceDetector) {
    if (!mShouldDetectFaces) {
      return;
    }

    RNCameraViewHelper.emitFaceDetectionErrorEvent(this, faceDetector);
  }

  public void onFaceDetectingTaskCompleted() {
    faceDetectorTaskLock = false;
  }

  public void onBarcodeDetectionError(RNBarcodeDetector barcodeDetector) {
    if (!mShouldGoogleDetectBarcodes) {
      return;
    }

    RNCameraViewHelper.emitBarcodeDetectionErrorEvent(this, barcodeDetector);
  }

  public void onBarcodeDetectingTaskCompleted() {
    googleBarcodeDetectorTaskLock = false;
  }

  /**
   *
   * Text recognition
   */

  public void setShouldRecognizeText(boolean shouldRecognizeText) {
    this.mShouldRecognizeText = shouldRecognizeText;
    mCamera2.setScanning(mShouldDetectFaces || mShouldGoogleDetectBarcodes || mShouldScanBarCodes || mShouldRecognizeText);
  }

  public void onTextRecognized(WritableArray serializedData) {
    if (!mShouldRecognizeText) {
      return;
    }

    RNCameraViewHelper.emitTextRecognizedEvent(this, serializedData);
  }

  public void onTextRecognizerTaskCompleted() {
    textRecognizerTaskLock = false;
  }

  /**
  *
  * End Text Recognition */

  public void onHostResume() {
    if (hasCameraPermissions()) {
      mBgHandler.post(new Runnable() {
        @Override
        public void run() {
          if ((mIsPaused && !isCameraOpened()) || mIsNew) {
            mIsPaused = false;
            mIsNew = false;
            mCamera2.start();
          }
        }
      });
    } else {
      RNCameraViewHelper.emitMountErrorEvent(this, "Camera permissions not granted - component could not be rendered.");
    }
  }

  public boolean isCameraOpened() {
    return mCamera2.isCameraOpened();
  }

  public void onHostPause() {
    if (!mIsPaused && isCameraOpened()) {
      mIsPaused = true;
      mCamera2.stop();
    }
  }

  public void pausePreview() {
    mCamera2.pausePreview();
  }

  public void resumePreview() {
    mCamera2.resumePreview();
  }

  public Set<AspectRatio> getSupportedAspectRatios() {
    return mCamera2.getSupportedAspectRatios();
  }

  public void onHostDestroy() {
    if (mFaceDetector != null) {
      mFaceDetector.release();
    }
    mThemedReactContext.removeLifecycleEventListener(this);

    // camera release can be quite expensive. Run in on bg handler
    // and cleanup last once everything has finished
    mBgHandler.post(new Runnable() {
        @Override
        public void run() {
          mCamera2.stop();
          cleanup();
        }
      });
  }

  public void cleanup(){
    if(mBgThread != null){
      mBgThread.quitSafely();
      mBgThread = null;
    }
    rnCameraView = null;
  }

  private void onZoom(float scale){
    float currentZoom=mCamera2.getZoom();
    float nextZoom=currentZoom+(scale-1.0f);

    if(nextZoom > currentZoom){
      mCamera2.setZoom(Math.min(nextZoom,1.0f));
    }else{
      mCamera2.setZoom(Math.max(nextZoom,0.0f));
    }

  }

  private boolean hasCameraPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      int result = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA);
      return result == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }
  private int scalePosition(float raw){
    Resources resources = getResources();
    Configuration config = resources.getConfiguration();
    DisplayMetrics dm = resources.getDisplayMetrics();
    return (int)(raw/ dm.density);
  }

  private class CallbackBridge implements RNCamCallback {

    private final ArrayList<Callback> mCallbacks = new ArrayList<>();

    private boolean mRequestLayoutOnOpen;

    CallbackBridge() {
    }

    public void add(Callback callback) {
      mCallbacks.add(callback);
    }

    @Override
    public void onCameraOpened() {
      if (mRequestLayoutOnOpen) {
        mRequestLayoutOnOpen = false;
        requestLayout();
      }
      for (Callback callback : mCallbacks) {
        callback.onCameraOpened(RNCameraView.this);
      }
    }

    @Override
    public void onCameraClosed() {
      for (Callback callback : mCallbacks) {
        callback.onCameraClosed(RNCameraView.this);
      }
    }

    @Override
    public void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation) {
      for (Callback callback : mCallbacks) {
        callback.onPictureTaken(RNCameraView.this, data, deviceOrientation, softwareRotation);
      }
    }

    @Override
    public void onFramePreview(Image image, int width, int height, int orientation) {
      for (Callback callback : mCallbacks) {
        callback.onFramePreview(RNCameraView.this, image, width, height, orientation);
      }
    }

    @Override
    public void onMountError() {
      for (Callback callback : mCallbacks) {
        callback.onMountError(RNCameraView.this);
      }
    }
  }

  @NonNull
  private TextureViewPreview createPreview(Context context) {
    TextureViewPreview preview;
    preview = new TextureViewPreview(context, this);
    return preview;
  }

  private GestureDetector.SimpleOnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener(){
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      RNCameraViewHelper.emitTouchEvent(RNCameraView.this,false,scalePosition(e.getX()),scalePosition(e.getY()));
      return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
      RNCameraViewHelper.emitTouchEvent(RNCameraView.this,true,scalePosition(e.getX()),scalePosition(e.getY()));
      return true;
    }
  };
  private ScaleGestureDetector.OnScaleGestureListener onScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
      onZoom(scaleGestureDetector.getScaleFactor());
      return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
      onZoom(scaleGestureDetector.getScaleFactor());
      return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
    }

  };

  @SuppressWarnings("UnusedParameters")
  public abstract static class Callback {

    /**
     * Called when camera is opened.
     *
     * @param cameraView The associated {@link RNCameraView}.
     */
    public void onCameraOpened(RNCameraView cameraView) {}

    /**
     * Called when camera is closed.
     *
     * @param cameraView The associated {@link RNCameraView}.
     */
    public void onCameraClosed(RNCameraView cameraView) {}

    /**
     * Called when a picture is taken.
     *
     * @param cameraView The associated {@link RNCameraView}.
     * @param data       JPEG data.
     */
    public void onPictureTaken(RNCameraView cameraView, byte[] data, int deviceOrientation, int softwareRotation) {}

    public void onFramePreview(RNCameraView cameraView, Image image, int width, int height, int orientation) {}

    public void onMountError(RNCameraView cameraView) {}
  }

}
