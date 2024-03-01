package com.rncamerademo.nativemodules.camera;

import android.content.pm.PackageManager;

import com.facebook.react.bridge.*;
import com.facebook.react.uimanager.UIManagerModule;

import com.rncamerademo.nativemodules.camera.utils.ScopedContext;

import java.io.File;


public class CameraModule extends ReactContextBaseJavaModule {

  private ScopedContext mScopedContext;

  public CameraModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mScopedContext = new ScopedContext(reactContext);
  }

  @Override
  public String getName() {
    return "CameraModule";
  }

  @ReactMethod
  public void takePictureAsync(final ReadableMap options, final Promise promise) {
    final ReactApplicationContext context = getReactApplicationContext();
    final File cacheDirectory = mScopedContext.getCacheDirectory();
    UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
    uiManager.addUIBlock(nativeViewHierarchyManager -> {
        RNCameraView cameraView = RNCameraView.getInstance(context);
        try {
            if (cameraView.isCameraOpened()) {
              cameraView.takePicture(options, promise, cacheDirectory);
            } else {
              promise.reject("E_CAMERA_UNAVAILABLE", "Camera is not running");
            }
        }
        catch (Exception e) {
          promise.reject("E_TAKE_PICTURE_FAILED", e.getMessage());
        }
    });
  }

  @ReactMethod
  public void hasTorch(final Promise promise) {
      promise.resolve(getReactApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH));
  }
}
