package com.rncamerademo.nativemodules.camera;

import android.Manifest;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.*;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

import com.google.zxing.BarcodeFormat;

import com.rncamerademo.nativemodules.camera.utils.BarcodeFormatUtils;
import com.rncamerademo.nativemodules.camera.utils.ScopedContext;
import com.rncamerademo.nativemodules.camera.Size;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;


public class CameraModule extends ReactContextBaseJavaModule {
  private static final String TAG = "rncameranativemodule";

  private ScopedContext mScopedContext;
  static final int VIDEO_2160P = 0;
  static final int VIDEO_1080P = 1;
  static final int VIDEO_720P = 2;
  static final int VIDEO_480P = 3;
  static final int VIDEO_4x3 = 4;

  static final int GOOGLE_VISION_BARCODE_MODE_NORMAL = 0;
  static final int GOOGLE_VISION_BARCODE_MODE_ALTERNATE = 1;
  static final int GOOGLE_VISION_BARCODE_MODE_INVERTED = 2;

  public static final Map<String, Object> VALID_BARCODE_TYPES =
      Collections.unmodifiableMap(new HashMap<>() {
        {
          put("aztec", BarcodeFormat.AZTEC.toString());
          put("ean13", BarcodeFormat.EAN_13.toString());
          put("ean8", BarcodeFormat.EAN_8.toString());
          put("qr", BarcodeFormat.QR_CODE.toString());
          put("pdf417", BarcodeFormat.PDF_417.toString());
          put("upc_e", BarcodeFormat.UPC_E.toString());
          put("datamatrix", BarcodeFormat.DATA_MATRIX.toString());
          put("code39", BarcodeFormat.CODE_39.toString());
          put("code93", BarcodeFormat.CODE_93.toString());
          put("interleaved2of5", BarcodeFormat.ITF.toString());
          put("codabar", BarcodeFormat.CODABAR.toString());
          put("code128", BarcodeFormat.CODE_128.toString());
          put("maxicode", BarcodeFormat.MAXICODE.toString());
          put("rss14", BarcodeFormat.RSS_14.toString());
          put("rssexpanded", BarcodeFormat.RSS_EXPANDED.toString());
          put("upc_a", BarcodeFormat.UPC_A.toString());
          put("upc_ean", BarcodeFormat.UPC_EAN_EXTENSION.toString());
        }
      });

  public CameraModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mScopedContext = new ScopedContext(reactContext);
  }

  public ScopedContext getScopedContext() {
    return mScopedContext;
  }

  @Override
  public String getName() {
    return "CameraModule";
  }

  @Nullable
  @Override
  public Map<String, Object> getConstants() {
    return Collections.unmodifiableMap(new HashMap<String, Object>() {
      {
        put("Type", getTypeConstants());
        put("FlashMode", getFlashModeConstants());
        put("AutoFocus", getAutoFocusConstants());
        put("WhiteBalance", getWhiteBalanceConstants());
        put("VideoQuality", getVideoQualityConstants());
        put("BarCodeType", getBarCodeConstants());
        put("FaceDetection", Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("Mode", getFaceDetectionModeConstants());
            put("Landmarks", getFaceDetectionLandmarksConstants());
            put("Classifications", getFaceDetectionClassificationsConstants());
          }

          private Map<String, Object> getFaceDetectionModeConstants() {
            return Collections.unmodifiableMap(new HashMap<String, Object>() {
              {
                put("fast", RNFaceDetector.FAST_MODE);
                put("accurate", RNFaceDetector.ACCURATE_MODE);
              }
            });
          }

          private Map<String, Object> getFaceDetectionClassificationsConstants() {
            return Collections.unmodifiableMap(new HashMap<String, Object>() {
              {
                put("all", RNFaceDetector.ALL_CLASSIFICATIONS);
                put("none", RNFaceDetector.NO_CLASSIFICATIONS);
              }
            });
          }

          private Map<String, Object> getFaceDetectionLandmarksConstants() {
            return Collections.unmodifiableMap(new HashMap<String, Object>() {
              {
                put("all", RNFaceDetector.ALL_LANDMARKS);
                put("none", RNFaceDetector.NO_LANDMARKS);
              }
            });
          }
        }));
        put("GoogleVisionBarcodeDetection", Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("BarcodeType", BarcodeFormatUtils.REVERSE_FORMATS);
            put("BarcodeMode", getGoogleVisionBarcodeModeConstants());
          }
        }));
        put("Orientation", Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
              put("auto", Constants.ORIENTATION_AUTO);
              put("portrait", Constants.ORIENTATION_UP);
              put("portraitUpsideDown", Constants.ORIENTATION_DOWN);
              put("landscapeLeft", Constants.ORIENTATION_LEFT);
              put("landscapeRight", Constants.ORIENTATION_RIGHT);
            }
        }));
      }

      private Map<String, Object> getTypeConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("front", Constants.FACING_FRONT);
            put("back", Constants.FACING_BACK);
          }
        });
      }

      private Map<String, Object> getFlashModeConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("off", Constants.FLASH_OFF);
            put("on", Constants.FLASH_ON);
            put("auto", Constants.FLASH_AUTO);
            put("torch", Constants.FLASH_TORCH);
          }
        });
      }

      private Map<String, Object> getAutoFocusConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("on", true);
            put("off", false);
          }
        });
      }

      private Map<String, Object> getWhiteBalanceConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("auto", Constants.WB_AUTO);
            put("cloudy", Constants.WB_CLOUDY);
            put("sunny", Constants.WB_SUNNY);
            put("shadow", Constants.WB_SHADOW);
            put("fluorescent", Constants.WB_FLUORESCENT);
            put("incandescent", Constants.WB_INCANDESCENT);
          }
        });
      }

      private Map<String, Object> getVideoQualityConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("2160p", VIDEO_2160P);
            put("1080p", VIDEO_1080P);
            put("720p", VIDEO_720P);
            put("480p", VIDEO_480P);
            put("4:3", VIDEO_4x3);
          }
        });
      }

      private Map<String, Object> getGoogleVisionBarcodeModeConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
          {
            put("NORMAL", GOOGLE_VISION_BARCODE_MODE_NORMAL);
            put("ALTERNATE", GOOGLE_VISION_BARCODE_MODE_ALTERNATE);
            put("INVERTED", GOOGLE_VISION_BARCODE_MODE_INVERTED);
          }
        });
      }

      private Map<String, Object> getBarCodeConstants() {
        return VALID_BARCODE_TYPES;
      }
    });
  }

  @ReactMethod
  public void takePictureAsync(final ReadableMap options, final Promise promise) {
    final ReactApplicationContext context = getReactApplicationContext();
    final File cacheDirectory = mScopedContext.getCacheDirectory();
    UIManagerModule uiManager = context.getNativeModule(UIManagerModule.class);
    uiManager.addUIBlock(nativeViewHierarchyManager -> {
//          RNCameraView cameraView = (RNCameraView) nativeViewHierarchyManager.resolveView(viewTag);
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
