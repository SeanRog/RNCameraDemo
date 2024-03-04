package com.rncamerademo.nativemodules.camera;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.os.AsyncTask;
import android.widget.FrameLayout;

import com.facebook.react.bridge.*;

import com.rncamerademo.nativemodules.camera.tasks.*;
import com.rncamerademo.nativemodules.camera.utils.HelperFunctions;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Singleton that manages the Camera2 instance
 */
public class RNCameraView extends FrameLayout implements LifecycleEventListener, PictureSavedDelegate {

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


  private boolean mIsPaused = false;
  private boolean mIsNew = true;

  // Scanning-related properties
  private boolean mShouldRecognizeText = false;
  private boolean mShouldReadBarcodes = false;
  private boolean mShouldDetectFaces = false;

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
        HelperFunctions.emitPictureTakenEvent(cameraView);
      }
    });
  }

  public static RNCameraView getInstance(ReactContext themedReactContext) {
    if (rnCameraView == null) {
      rnCameraView = new RNCameraView(themedReactContext);
    }
    return rnCameraView;
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
    HelperFunctions.emitPictureSavedEvent(this, response);
  }

  public void setFaceDetectionLandmarks(int landmarks) {
    // todo move logic to camera2
//    mFaceDetectionLandmarks = landmarks;
//    if (mFaceDetector != null) {
//      mFaceDetector.setLandmarkType(landmarks);
//    }
  }

  public void setFaceDetectionClassifications(int classifications) {
    // todo move logic to camera2
//    mFaceDetectionClassifications = classifications;
//    if (mFaceDetector != null) {
//      mFaceDetector.setClassificationType(classifications);
//    }
  }

  public void setFaceDetectionMode(int mode) {
    // todo move logic to camera2
//    mFaceDetectorMode = mode;
//    if (mFaceDetector != null) {
//      mFaceDetector.setMode(mode);
//    }
  }

  public void setTracking(boolean trackingEnabled) {
    // todo move logic to camera2
//    mTrackingEnabled = trackingEnabled;
//    if (mFaceDetector != null) {
//      mFaceDetector.setTracking(trackingEnabled);
//    }
  }

  /**
   *
   * Text recognition
   */

  public void setShouldRecognizeText(boolean shouldRecognizeText) {
    this.mShouldRecognizeText = shouldRecognizeText;
    mCamera2.setScanning(mShouldRecognizeText, mShouldReadBarcodes, mShouldDetectFaces);
  }
  public void setShouldReadBarcodes(boolean shouldReadBarcodes) {
    this.mShouldReadBarcodes = shouldReadBarcodes;
    mCamera2.setScanning(mShouldRecognizeText, mShouldReadBarcodes, mShouldDetectFaces);
  }
  public void setShouldDetectFaces(boolean shouldDetectFaces) {
    this.mShouldDetectFaces = shouldDetectFaces;
    mCamera2.setScanning(mShouldRecognizeText, mShouldReadBarcodes, mShouldDetectFaces);
  }

  /**
  *
  * End Text Recognition */

  public void onHostResume() {
    mBgHandler.post(() -> {
      if ((mIsPaused && !isCameraOpened()) || mIsNew) {
        mIsPaused = false;
        mIsNew = false;
        mCamera2.start();
      }
    });
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

  public void onHostDestroy() {

    // todo move logic to camera2
//    if (mFaceDetector != null) {
//      mFaceDetector.release();
//    }

    mThemedReactContext.removeLifecycleEventListener(this);

    // camera release can be quite expensive. Run in on bg handler
    // and cleanup last once everything has finished
    mBgHandler.post(() -> {
      mCamera2.stop();
      cleanup();
    });
  }

  public void cleanup(){
    if(mBgThread != null){
      mBgThread.quitSafely();
      mBgThread = null;
    }
    rnCameraView = null;
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
    public void onPictureTaken(byte[] data, int deviceOrientation, int softwareRotation) {
      for (Callback callback : mCallbacks) {
        callback.onPictureTaken(RNCameraView.this, data, deviceOrientation, softwareRotation);
      }
    }
  }

  @NonNull
  private TextureViewPreview createPreview(Context context) {
    TextureViewPreview preview;
    preview = new TextureViewPreview(context, this);
    return preview;
  }

  @SuppressWarnings("UnusedParameters")
  public abstract static class Callback {

    /**
     * Called when a picture is taken.
     *
     * @param cameraView The associated {@link RNCameraView}.
     * @param data       JPEG data.
     */
    public void onPictureTaken(RNCameraView cameraView, byte[] data, int deviceOrientation, int softwareRotation) {}
  }

}
